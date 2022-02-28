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

        Solution.OBJECTIVE = Solution.FMAX;
        Solution.iter_limit = 100000;

        String path = "C:\\Users\\tunch\\OneDrive\\Belgeler\\instances\\",
                sets[] = {"SSP-NPM-I\\", "SSP-NPM-II\\"};
        int set_no = 0; // Number zero indicates small instances and number one indicates large ones.
        File folder = new File(path + sets[set_no]);
        File files[] = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".csv");
            }
        });
        for (int f = 0; f < files.length; f++) {
            File file = files[f];
            Solution.termination = System.currentTimeMillis() + 600000;
            Solution result = tabusearch(file);
            String line = f + ";" + file.getName() + ";" + (Solution.M + ";" + Solution.J + ";" + Solution.T
                    + ";" + result.fitness).replace('.', ',');
            System.out.println(line);
        }

    }

    private static Solution tabusearch(File filename) {
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
        int elite_size = 50;

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
        Solution elite_solutions[] = new Solution[elite_size], best_solution[] = new Solution[1];
        int worsSolutionIndex[] = {-1};
        Worker worker = new Worker(null, elite_solutions, best_solution, worsSolutionIndex, 0);
        for (int s = 0; s < elite_size; s++) {
            elite_solutions[s] = new Solution(Solution.IEACT);
            elite_solutions[s].initialize();
            elite_solutions[s] = worker.localSearch(elite_solutions[s]);
            if (best_solution[0] == null || best_solution[0].fitness > elite_solutions[s].fitness) {
                best_solution[0] = elite_solutions[s];
            }
            if (worsSolutionIndex[0] == -1 || elite_solutions[worsSolutionIndex[0]].fitness < elite_solutions[s].fitness) {
                worsSolutionIndex[0] = s;
            }
        }
        worker.tabuSearch();
        return best_solution[0];

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
