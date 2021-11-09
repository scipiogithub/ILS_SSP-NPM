/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gals;

import java.util.ArrayList;
import java.util.Random;
import static gals.Solution.J;
import static gals.Solution.M;
import static gals.Solution.NONE;
import static gals.Solution.iter_limit;
import static gals.Solution.local_limit;
import static gals.Solution.operator;


public class Worker implements Runnable{
 
    final Object lock;
    final Random rnd;
    Solution population[], bestSolution[];
    int worstSolutionPointer[];

    public Worker(Object lock, Solution[] population, Solution[] bestSolution, int worstSolutionPointer[]) {
        this.lock = lock;
        this.population = population;
        this.bestSolution = bestSolution;
        this.worstSolutionPointer = worstSolutionPointer;
        rnd = new Random();
    }
    
    
    public Solution crossover() {
        Solution firstparent = null, otherparent = null;
        synchronized (lock) {
            firstparent = population[rnd.nextInt(population.length)];
            otherparent = population[rnd.nextInt(population.length)];
            while (otherparent == firstparent) {
                otherparent = population[rnd.nextInt(population.length)];
            }
        }
        return crossover(firstparent, otherparent);
    }
    
    private Solution crossover(Solution firstparent, Solution otherparent) {
        Solution child = new Solution(NONE);
        int totalassignment = 0;
        for (int m = 0; m < M; m++) {
            int point = firstparent.machines[m].jobs.isEmpty() ? 0 : (firstparent.machines[m].jobs.size() > 1 ? rnd.nextInt(firstparent.machines[m].jobs.size() - 1) + 1 : 1);
            child.machines[m] = firstparent.machines[m].copy(point);
            totalassignment += point;
        }
        for (int m = 0; m < M; m++) {
            ArrayList<Job> parentjobs = otherparent.machines[m].jobs;
            for (int j = 0; j < parentjobs.size(); j++) {
                Job newjob = parentjobs.get(j);
                if (!child.jobExists(newjob) && child.machines[m].jobCanbeadded(newjob)) {
                    child.machines[m].addJob(newjob);
                    totalassignment += 1;
                }
            }
        }
        while (totalassignment < J) {
            for (int m = 0; totalassignment < J && m < M; m++) {
                for (Job job : otherparent.machines[m].jobs) {
                    if (!child.jobExists(job) && child.machines[m].jobCanbeadded(job)) {
                        totalassignment += 1;
                        child.machines[m].addJob(job);
                        break;
                    }
                }
            }
        }

        child.setFitness();
        return child;
    }
    
    private Solution localSearch(Solution solution) {
        int counter = 0;
        while (counter++ < local_limit) {
            int m1 = rnd.nextInt(M), m2 = rnd.nextInt(M);
            double op = rnd.nextDouble();
            if (op > operator[0] + operator[1] && solution.machines[m1].jobs.isEmpty())
                continue;
            if (op <= operator[0] + operator[1] && (solution.machines[m1].jobs.isEmpty() || solution.machines[m2].jobs.isEmpty())) {
                continue;
            }
            int j1 = rnd.nextInt(solution.machines[m1].jobs.size()),
                    j2 = op <= operator[0] + operator[1] ? rnd.nextInt(solution.machines[m2].jobs.size()) : 0;

            Solution ls = op <= operator[0] ? solution.swap(m1, j1, m2, j2) : 
                    (op <= operator[0] + operator[1] ? solution.insert(m1, j1, m2, j2) : solution.reset(m1, j1));
            if (ls.fitness < solution.fitness) {
                counter = 0;
                solution = ls;
            }
        }
        return solution;
    }
    
    
    @Override
    public void run() {
        int counter = 0;
        while (counter++ < iter_limit) {
            Solution child = crossover();
            child = localSearch(child);
            synchronized (lock) {
                if (population[worstSolutionPointer[0]].fitness > child.fitness) {
                    population[worstSolutionPointer[0]] = child;
                    if (bestSolution[0].fitness > child.fitness) {
                        bestSolution[0] = child;
                    }
                    worstSolutionPointer[0] = 0;
                    for (int p = 1; p < population.length; p++) {
                        if (population[worstSolutionPointer[0]].fitness < population[p].fitness) {
                            worstSolutionPointer[0] = p;
                        }
                    }
                }
            }

        }

    }

}
