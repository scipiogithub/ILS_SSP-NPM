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

public class Worker implements Runnable {

    final Object lock;
    final Random rnd;
    Solution population[], bestSolution[];
    int worstSolutionPointer[], id;
    static int tabutenure;
    

    public Worker(Object lock, Solution[] population, Solution[] bestSolution, int worstSolutionPointer[], int id) {
        this.lock = lock;
        this.population = population;
        this.bestSolution = bestSolution;
        this.worstSolutionPointer = worstSolutionPointer;
        this.id = id;

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
            child.machines[m] = new Machine(firstparent.machines[m].index, firstparent.machines[m].capacity, firstparent.machines[m].switchtime, firstparent.machines[m].rnd);
            for (int j = 0; j < point; j++) {
                child.machines[m].jobs.add(firstparent.machines[m].jobs.get(j));
            }
            //child.machines[m] = firstparent.machines[m].copy(point);
            totalassignment += point;
        }
        for (int m = 0; m < M; m++) {
            ArrayList<Job> parentjobs = otherparent.machines[m].jobs;
            for (int j = 0; j < parentjobs.size(); j++) {
                Job newjob = parentjobs.get(j);
                if (!child.jobExists(newjob) && child.machines[m].jobCanbeadded(newjob)) {
                    child.machines[m].jobs.add(newjob);//addJob(newjob);
                    totalassignment += 1;
                }
            }
        }
        while (totalassignment < J) {
            for (int m = 0; totalassignment < J && m < M; m++) {
                for (Job job : otherparent.machines[m].jobs) {
                    if (!child.jobExists(job) && child.machines[m].jobCanbeadded(job)) {
                        totalassignment += 1;
                        child.machines[m].jobs.add(job);//addJob(job);
                        break;
                    }
                }
            }
        }
        for (Machine m : child.machines) {
            m.setFit();
        }

        child.setFitness();
        return child;
    }

    public Solution localSearch(Solution solution) {
        int counter = 0;
        while (counter++ < local_limit) {
            int m1 = rnd.nextInt(M), m2 = rnd.nextInt(M);
            double op = rnd.nextDouble();
            if (op > operator[2] && solution.machines[m1].jobs.isEmpty()) {
                continue;
            }
            if (op <= operator[2] && (solution.machines[m1].jobs.isEmpty() || solution.machines[m2].jobs.isEmpty())) {
                continue;
            }
            int j1 = rnd.nextInt(solution.machines[m1].jobs.size()),
                    j2 = op <= operator[2] ? rnd.nextInt(solution.machines[m2].jobs.size()) : 0,
                    j3 = op > operator[1] && op <= operator[2] ? rnd.nextInt(solution.machines[m2].jobs.size()) : -1;
            if (j3 >= 0 && j2 > j3) {
                int temp = j2;
                j2 = j3;
                j3 = temp;
            }

            Solution ls = op <= operator[0] ? solution.swap(m1, j1, m2, j2) //: solution.insert(m1, j1, m2, j2)
                    : (op <= operator[1] ? solution.insert(m1, j1, m2, j2)
                            : solution.insert(m1, j1, m2, j2, j3));
            //: solution.reset(m1, j1)));
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
        long t = System.currentTimeMillis();
        while (counter++ < iter_limit && t < Solution.termination) {
            Solution child = new Solution(NONE);//
            child.copy(population[rnd.nextInt(population.length)]);
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
            t = System.currentTimeMillis();
        }
    }


}
