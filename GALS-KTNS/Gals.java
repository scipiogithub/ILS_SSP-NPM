package gals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gals {

    static int popsize;

    public static void main(String[] args) throws Exception {

        String path = "C:\\Users\\tunch\\OneDrive\\Belgeler\\instances\\",
                sets[] = {"SSP-NPM-I\\", "SSP-NPM-II\\"};// converging[] = {"ins3_m=2_j=10_t=10_var=3.csv", "ins141_m=3_j=20_t=20_var=1.csv"};

        int set_no = 1; // Number one indicates small instances and number two indicates large ones.
        File folder = new File(path + sets[set_no]);
        File files[] = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".csv");
            }
        });
        popsize = 50;
        Solution.OBJECTIVE = Solution.FMAX;
        Solution.iter_limit = 4000;
        Solution.local_limit = 90;

        double average = 0, time = 0;

        for (int f = 0; f < files.length; f++) {
            File file = files[f];

            Solution.operator[0] = 0.45;
            Solution.operator[1] = 0.55;
            Solution.operator[2] = 0;
            Solution.operator[1] += Solution.operator[0];
            Solution.operator[2] += Solution.operator[1];

            Object result[] = run(file);
            if (result == null) {
                continue;
            }
            Solution best = (Solution) result[0];
            double newtime = ((long) result[1]) / 1000.0;
            String line = f + ";" + file.getName() + ";" + (Solution.M + ";" + Solution.J + ";" + Solution.T
                    + ";" + best.fitness + ";" + newtime).replace('.', ',');
            System.out.println(line);
            //System.out.println(best);
            time += newtime;
            average += best.fitness;
        }
        average /= files.length;
        time /= files.length;
        System.out.println(average + ";" + time);

    }


    private static Object[] run(File filename) throws InterruptedException {
        ArrayList data = readFile(filename);
        int M = ((int[]) data.get(0))[0], J = ((int[]) data.get(0))[1],
                T = ((int[]) data.get(0))[2], C[] = (int[]) data.get(1),
                sw[] = (int[]) data.get(2), p[][] = (int[][]) data.get(3);

        Solution.M = M;
        Solution.J = J;
        Solution.T = T;
        Solution.C = C;
        Solution.sw = sw;
        Solution.p = p;

        ArrayList<Integer> TS[] = (ArrayList<Integer>[]) data.get(4);
        Job allJobs[] = new Job[J];
        for (int j = 0; j < J; j++) {
            boolean flag = true;
            while (flag) {
                flag = false;
                for (int i = 0; i < TS[j].size() - 1; i++) {
                    for (int k = i + 1; k < TS[j].size(); k++) {
                        if (TS[j].get(i).intValue() > TS[j].get(k).intValue()) {
                            flag = true;
                            Integer temp = TS[j].get(i);
                            TS[j].set(i, TS[j].get(k));
                            TS[j].set(k, temp);
                        }
                    }
                }
            }
            Integer tools[] = new Integer[TS[j].size()];
            for (int i = 0; i < tools.length; i++) {
                tools[i] = TS[j].get(i);
            }
            allJobs[j] = new Job(j, p[j], tools);
        }
        Solution.allJobs = allJobs;
        Solution bestSolution[] = new Solution[1];
        int worsSolutionIndex[] = new int[1];
        worsSolutionIndex[0] = -1;
        Object lock = new Object();
        Solution population[] = new Solution[popsize];
        long start = System.currentTimeMillis();
        Solution.termination = start + 600000 * 4;
        for (int s = 0; s < popsize; s++) {
            population[s] = new Solution(s == 0 ? Solution.IEACT : Solution.RANDOM);
            population[s].initialize();
            if (bestSolution[0] == null || bestSolution[0].fitness > population[s].fitness) {
                bestSolution[0] = population[s];
            }
            if (worsSolutionIndex[0] == -1 || population[worsSolutionIndex[0]].fitness < population[s].fitness) {
                worsSolutionIndex[0] = s;
            }
        }
        Worker[] workers = new Worker[5];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new Worker(lock, population, bestSolution, worsSolutionIndex, i);
        }
        ExecutorService es = Executors.newCachedThreadPool();
        for (Worker i : workers) {
            es.execute(i);
        }
        es.shutdown();
        es.awaitTermination(1, TimeUnit.DAYS);

        start = System.currentTimeMillis() - start;
        Object result[] = new Object[2];
        result[0] = bestSolution[0];
        result[1] = start;
        return result;
    }

    private static ArrayList readFile(File filename) {
        BufferedReader file;
        ArrayList result = new ArrayList();
        try {
            file = new BufferedReader(new FileReader(filename));
            String splittedline[] = file.readLine().split(";");
            int MJT[] = new int[3];
            for (int i = 0; i < 3; i++) {
                MJT[i] = Integer.parseInt(splittedline[i]);
            }
            result.add(MJT);
            int C[] = new int[MJT[0]], sw[] = new int[MJT[0]];
            splittedline = file.readLine().split(";");
            for (int i = 0; i < C.length; i++) {
                C[i] = Integer.parseInt(splittedline[i]);
            }
            result.add(C);
            splittedline = file.readLine().split(";");
            for (int i = 0; i < sw.length; i++) {
                sw[i] = Integer.parseInt(splittedline[i]);
            }
            result.add(sw);
            int p[][] = new int[MJT[1]][MJT[0]];
            for (int m = 0; m < MJT[0]; m++) {
                splittedline = file.readLine().split(";");
                for (int j = 0; j < MJT[1]; j++) {
                    p[j][m] = Integer.parseInt(splittedline[j]);
                }
            }
            result.add(p);
            ArrayList<Integer> T[] = new ArrayList[MJT[1]];
            for (int t = 0; t < MJT[2]; t++) {
                splittedline = file.readLine().split(";");

                for (int j = 0; j < MJT[1]; j++) {
                    if (splittedline[j].equals("0")) {
                        continue;
                    }
                    if (T[j] == null) {
                        T[j] = new ArrayList<>();
                    }
                    T[j].add(Integer.valueOf(t));
                }
            }
            result.add(T);
            file.close();
        } catch (Exception ex) {
            System.out.println(ex);
        }

        return result;
    }

}
