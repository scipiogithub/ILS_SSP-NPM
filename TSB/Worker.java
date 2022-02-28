
package gals;

import java.util.ArrayList;
import java.util.Random;
import static gals.Solution.J;
import static gals.Solution.M;
import static gals.Solution.NONE;
import static gals.Solution.iter_limit;
import static gals.Solution.local_limit;
import static gals.Solution.operator;

public class Worker {

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
            if (ls.fitness < solution.fitness) {
                counter = 0;
                solution = ls;
            }
        }
        return solution;
    }

    


    private void setProb(double prob[][], Solution solution) {
        for (Machine m : solution.machines) {
            for (int j = 0; j < m.jobs.size() - 1; j++) {
                Job job = m.jobs.get(j), nextjob = m.jobs.get(j + 1);
                double val = 1000.0 / solution.fitness;
                prob[job.index][nextjob.index] += val;
            }
        }
    }

    private double getProb(double prob[][], int tabu[][], int counter) {
        double total = 0;
        for (int i = 0; i < J; i++) {
            for (int j = 0; j < J; j++) {
                if (tabu[i][j] < counter) {
                    total += prob[i][j];
                }
            }
        }
        return total;
    }

    private int[] selectJobs(double prob[][], int tabu[][], int counter) {
        double total = getProb(prob, tabu, counter), r = rnd.nextDouble() * total, mytotal = 0;
        for (int j = 0; j < J; j++) {
            for (int j2 = 0; j2 < J; j2++) {
                if (tabu[j][j2] < counter) {
                    mytotal += prob[j][j2];
                    if (r <= mytotal) {
                        tabu[j][j2] = counter + tabutenure;
                        int result[] = {j, j2};
                        return result;
                    }
                }
            }
        }
        return null;
    }

    private int[] getJobMachineIndex(int jobno, Solution solution) {
        for (int mi = 0; mi < M; mi++) {
            Machine m = solution.machines[mi];
            for (int j = 0; j < m.jobs.size(); j++) {
                Job job = m.jobs.get(j);
                if (job.index == jobno) {
                    int result[] = {mi, j};
                    return result;
                }
            }
        }
        return null;
    }

    private boolean exists(int fitness) {
        for (Solution solution : population) {
            if (fitness == solution.fitness) {
                return true;
            }
        }
        return false;
    }

    private void updateBestworst() {
        for (int s = 0; s < population.length; s++) {
            if (bestSolution[0].fitness > population[s].fitness) {
                bestSolution[0] = population[s];
            }
            if (population[worstSolutionPointer[0]].fitness < population[s].fitness) {
                worstSolutionPointer[0] = s;
            }
        }
    }

    public void tabuSearch() {
        tabutenure = (int) (J * J * 0.2);
        Solution solution = new Solution(NONE);
        solution.copy(bestSolution[0]);
        int tabu[][] = new int[J][J];
        double prob[][] = new double[J][J];
        for (int i = 0; i < J; i++) {
            for (int j = 0; j < J; j++) {
                prob[i][j] = 0x0.0000000000001P-100;
            }
        }
        for (Solution s : population) {
            setProb(prob, s);
        }
        int counter = 0, limit = 0;
        long tm = System.currentTimeMillis();
        while (counter++ < iter_limit && tm < Solution.termination) {
            int jobs[] = selectJobs(prob, tabu, counter);
            int job1[] = getJobMachineIndex(jobs[0], solution), job2[] = getJobMachineIndex(jobs[1], solution);
            int m1 = job1[0], m2 = job2[0];

            if (solution.machines[m1].jobs.isEmpty() || solution.machines[m2].jobs.isEmpty()) {
                tm = System.currentTimeMillis();
                continue;
            }
            int j1 = job1[1], j2 = job2[1];
            if (j2 > 0) {
                solution = solution.swap(m1, j1, m2, j2 - 1);
                if (solution.fitness < population[worstSolutionPointer[0]].fitness && !exists(solution.fitness)) {
                    population[worstSolutionPointer[0]].copy(solution);
                    updateBestworst();
                    limit = 0;
                    setProb(prob, solution);
                }
            }
            solution = solution.insert(m2, j2, m1, j1);
            if (solution.fitness < population[worstSolutionPointer[0]].fitness && !exists(solution.fitness)) {
                population[worstSolutionPointer[0]].copy(solution);
                updateBestworst();
                limit = 0;
                setProb(prob, solution);
            }
            if (++limit > 1000) {
                solution.copy(bestSolution[0]);
                limit = 0;
            }
            tm = System.currentTimeMillis();
        }
    }

}
