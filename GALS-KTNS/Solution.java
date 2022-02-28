/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gals;

import java.util.ArrayList;
import java.util.Random;

public class Solution {

    static int M, J, T, C[], sw[], p[][], iter_limit, OBJECTIVE, local_limit;
    static double operator[] = {0, 0, 0};
    static long termination;
    final static int NONE = 0, IEACT = 1, RANDOM = 2, FMAX = 0, TFT = 1, TS = 2;
    static Job allJobs[];
    Machine machines[];

    int init_method, fitness;
    final Random rnd;

    public Solution(int init_method) {
        this.init_method = init_method;
        machines = new Machine[M];
        rnd = new Random();
        for (int m = 0; m < M; m++) {
            machines[m] = new Machine(m, C[m], sw[m], rnd);
        }

    }

    public void initialize() {
        if (init_method == IEACT) {
            IEACT();
        } else if (init_method == RANDOM) {
            RAND_INIT();
        }
        setFitness();

    }

    @Override
    public String toString() {
        String result = "Solution:";
        for (int m = 0; m < M; m++) {
            Machine machine = machines[m];
            result += machine + (m < M - 1 ? "," : "");
        }
        result += ", Objective:" + fitness;
        return result;
    }

    public void copy(Solution from) {
        for (int m = 0; m < M; m++) {
            machines[m] = from.machines[m].copyAll();
        }
        fitness = from.fitness;
    }

    public boolean jobExists(Job job) {
        for (int m = 0; m < M; m++) {
            Machine machine = machines[m];
            if (machine.jobs.contains(job)) {
                return true;
            }
        }
        return false;
    }

    public void setFitness() {
        int fmax = 0, tf = 0, ts = 0;
        for (Machine machine : machines) {
            tf += machine.totalflowtime;
            ts += machine.numberofswitches;
            if (OBJECTIVE == FMAX) {
                if (machine.flowtime > fmax) {
                    fmax = machine.flowtime;
                }
            }
        }
        fitness = OBJECTIVE == FMAX ? fmax : (OBJECTIVE == TFT ? tf : ts);
    }

    private void RAND_INIT() {
        ArrayList<Job> assignedJobs = new ArrayList<>();
        while (assignedJobs.size() < J) {
            int jobindex = rnd.nextInt(J);
            for (int j = 0; j < J; j++) {
                Job suitableJob = allJobs[(j + jobindex) % J];
                Machine suitableMachine = machines[rnd.nextInt(M)];
                if (!assignedJobs.contains(suitableJob) && suitableMachine.jobCanbeadded(suitableJob)) {
                    suitableMachine.jobs.add(suitableJob);
                    assignedJobs.add(suitableJob);
                    break;
                }
            }
        }
        for (Machine m : machines) {
            m.setFit();
        }
    }

    private void IEACT() {
        ArrayList<Job> assignedJobs = new ArrayList<>();
        while (assignedJobs.size() < J) {
            Job suitableJob = null;
            Machine suitableMachine = null;
            ArrayList<Object> bestCost = null;
            int minflowtime = Integer.MAX_VALUE;
            for (Job job : allJobs) {
                if (assignedJobs.contains(job)) {
                    continue;
                }
                for (Machine machine : machines) {
                    if (machine.jobCanbeadded(job)) {
                        ArrayList<Object> costs = machine.costofAddingJob(job);
                        int n_sw = ((Integer) costs.get(2)).intValue();
                        if (minflowtime > machine.flowtime + job.getProcessingTime(machine) + machine.switchtime * n_sw) {
                            suitableJob = job;
                            suitableMachine = machine;
                            minflowtime = machine.flowtime + job.getProcessingTime(machine) + machine.switchtime * n_sw;
                            bestCost = costs;
                        }
                    }
                }
            }
            //ArrayList<Integer> newslot = (ArrayList<Integer>) bestCost.get(0);
            //ArrayList<Integer> requiredTools = (ArrayList<Integer>) bestCost.get(1);
            suitableMachine.jobs.add(suitableJob);//addJob(suitableJob, newslot, requiredTools);
            assignedJobs.add(suitableJob);
        }
        for (Machine m : machines) {
            m.setFit();
        }

    }

    public Solution swap(int m1, int j1, int m2, int j2) {
        if (OBJECTIVE == FMAX && machines[m1].flowtime < fitness && machines[m2].flowtime < fitness) {
            for (int m = 0; m < M; m++) {
                if (machines[m].flowtime == fitness) {
                    m2 = m;
                    break;
                }
            }
            j2 = rnd.nextInt(machines[m2].jobs.size());
        }
        if (!machines[m1].jobCanbeadded(machines[m2].jobs.get(j2)) || !machines[m2].jobCanbeadded(machines[m1].jobs.get(j1))) {
            return this;
        }

        Solution newSolution = new Solution(NONE);
        for (int m = 0; m < M; m++) {
            if (m != m1 && m != m2) {
                newSolution.machines[m] = machines[m].copyAll();
            } else {
                for (int j = 0; j < machines[m].jobs.size(); j++) {
                    if (m == m1 && j == j1) {
                        newSolution.machines[m].jobs.add(machines[m2].jobs.get(j2));//addJob(machines[m2].jobs.get(j2));
                    } else if (m == m2 && j == j2) {
                        newSolution.machines[m].jobs.add(machines[m1].jobs.get(j1));//addJob(machines[m1].jobs.get(j1));
                    } else {
                        newSolution.machines[m].jobs.add(machines[m].jobs.get(j));//addJob(machines[m].jobs.get(j));
                    }
                }

            }
        }
        newSolution.machines[m1].setFit();
        if (m1 != m2) {
            newSolution.machines[m2].setFit();
        }
        newSolution.setFitness();
        return newSolution;
    }

    /*public Solution reset(int m1, int j1) {
        if (OBJECTIVE == FMAX && machines[m1].flowtime < fitness) {
            for (int m = 0; m < M; m++) {
                if (machines[m].flowtime == fitness) {
                    m1 = m;
                    break;
                }
            }
            j1 = rnd.nextInt(machines[m1].jobs.size());
        }

        Solution newSolution = new Solution(NONE);
        for (int m = 0; m < M; m++) {
            if (m != m1) {
                newSolution.machines[m] = machines[m].copyAll();
            } else {
                newSolution.machines[m] = machines[m].copy(j1);
                for (int j = j1; j < machines[m].jobs.size(); j++) {
                    newSolution.machines[m].addJob(machines[m].jobs.get(j));
                }
            }
        }
        newSolution.setFitness();
        return newSolution;
    }*/

    public Solution insert(int into, int beforejob, int from, int job) {
        if (OBJECTIVE == FMAX && machines[from].flowtime < fitness) {
            for (int m = 0; m < M; m++) {
                if (machines[m].flowtime == fitness) {
                    from = m;
                    break;
                }
            }
            job = rnd.nextInt(machines[from].jobs.size());
        }
        if (!machines[into].jobCanbeadded(machines[from].jobs.get(job))) {
            return this;
        }
        Solution newSolution = new Solution(NONE);
        for (int m = 0; m < M; m++) {
            if (m != into && m != from) {
                newSolution.machines[m] = machines[m].copyAll();
            } else {
                for (int j = 0; j < machines[m].jobs.size(); j++) {
                    if (m == into && j == beforejob) {
                        newSolution.machines[m].jobs.add(machines[from].jobs.get(job));//addJob(machines[from].jobs.get(job));
                        newSolution.machines[m].jobs.add(machines[m].jobs.get(j));//addJob(machines[m].jobs.get(j));
                    } else if (m != from || j != job) {
                        newSolution.machines[m].jobs.add(machines[m].jobs.get(j));//addJob(machines[m].jobs.get(j));
                    }
                }
            }
        }
        newSolution.machines[into].setFit();
        newSolution.machines[from].setFit();
        newSolution.setFitness();
        return newSolution;
    }
    
    //group insert

    public Solution insert(int into, int beforejob, int from, int job, int tojob) {
        if (OBJECTIVE == FMAX && machines[from].flowtime < fitness) {
            for (int m = 0; m < M; m++) {
                if (machines[m].flowtime == fitness) {
                    from = m;
                    break;
                }
            }
            job = rnd.nextInt(machines[from].jobs.size());
            tojob = rnd.nextInt(machines[from].jobs.size());
        }
        if (job > tojob) {
            int temp = job;
            job = tojob;
            tojob = temp;
        }
        for (int k = job; k <= tojob; k++) {
            if (!machines[into].jobCanbeadded(machines[from].jobs.get(k))) {
                return this;
            }
        }
        Solution newSolution = new Solution(NONE);
        for (int m = 0; m < M; m++) {
            if (m != into && m != from) {
                newSolution.machines[m] = machines[m].copyAll();
            } else {
                for (int j = 0; j < machines[m].jobs.size(); j++) {
                    if (m == into && j == beforejob) {
                        for (int k = job; k <= tojob; k++) {
                            newSolution.machines[m].jobs.add(machines[from].jobs.get(k));//addJob(machines[from].jobs.get(k));
                        }
                        newSolution.machines[m].jobs.add(machines[m].jobs.get(j));//addJob(machines[m].jobs.get(j));
                    } else if (m != from || j < job || j > tojob) {
                        newSolution.machines[m].jobs.add(machines[m].jobs.get(j));//addJob(machines[m].jobs.get(j));
                    }
                }
            }
        }
        newSolution.machines[into].setFit();
        newSolution.machines[from].setFit();
        newSolution.setFitness();
        return newSolution;
    }

}
