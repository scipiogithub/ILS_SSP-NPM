
package gals;

import java.util.ArrayList;


public class Job {

    int processtime[], index;
    Integer tools[];

    public Job(int index, int[] processtime, Integer[] tools) {
        this.index = index;
        this.processtime = processtime;
        this.tools = tools;
    }

    @Override
    public String toString() {
        String result = "Job:" + index + " TNJ(";
        sortTools();
        for (int t = 0; t < tools.length; t++) {
            result += tools[t] + (t < toolsize() - 1 ? "," : ")");
        }
        return result; 
    }

    private void sortTools() {
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int i = 0; i < toolsize() - 1; i++) {
                for (int j = i + 1; j < toolsize(); j++) {
                    if (tools[i] > tools[j]) {
                        int temp = tools[i];
                        tools[i] = tools[j];
                        tools[j] = temp;
                        flag = true;
                    }
                }
            }
        }
    }

    public int toolsize() {
        if (tools == null) {
            return 0;
        }
        return tools.length;
    }

    public boolean toolCanbeReplacedFromslotForMe(Integer tool) {
        for (Integer t : tools) {
            if (t == tool) {
                return false;
            }
        }
        return true;
    }

    public int getProcessingTime(Machine m) {
        return processtime[m.index];
    }

    public ArrayList<Integer> requiredTools(ArrayList<Integer> slot, Machine m) {

        ArrayList<Integer> result = new ArrayList<>(m.capacity);
        for (Integer t : tools) {
            if (!slot.contains(t)) {
                result.add(t);
            }
        }
        return result;
    }

}
