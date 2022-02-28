
package gals;

import java.util.ArrayList;
import java.util.Random;

public class Machine {

    final int capacity, switchtime, index;
    int flowtime, numberofswitches, totalflowtime;

    ArrayList<Job> jobs;
    ArrayList<ArrayList<Integer>> slots;
    ArrayList<Integer> flowtimes, removedTools, cumulativeswitches;
    final Random rnd;

    public Machine(int index, int capacity, int switchtime, Random rnd) {
        this.index = index;
        this.capacity = capacity;
        this.switchtime = switchtime;
        jobs = new ArrayList<>(Solution.J);
        slots = new ArrayList<>(Solution.J / Solution.M);
        flowtimes = new ArrayList<>(Solution.J / Solution.M);
        removedTools = new ArrayList<>(capacity);
        cumulativeswitches = new ArrayList<>(Solution.J / Solution.M);
        this.rnd = rnd;

    }

    @Override
    public String toString() {
        String result = "{M:" + index + " C:" + capacity + " [";
        for (int j = 0; j < jobs.size(); j++) {
            Job job = jobs.get(j);
            result += "(" + job + " Tools(";
            sortSlot(slots.get(j));
            for (int t = 0; t < slots.get(j).size(); t++) {
                result += slots.get(j).get(t) + (t < slots.get(j).size() - 1 ? "," : ")");
            }
            result += ")" + (j < jobs.size() - 1 ? ", " : "");
        }

        return result + "]}"; 
    }
    
    private void sortSlot(ArrayList<Integer> slot) {
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int i = 0; i < slot.size() - 1; i++) {
                for (int j = i + 1; j < slot.size(); j++) {
                    if (slot.get(i) > slot.get(j)) {
                        Integer temp = slot.get(i);
                        slot.set(i, slot.get(j));
                        slot.set(j, temp);
                        flag = true;
                    }
                }
            }
        }
    }

    public boolean jobCanbeadded(Job j) {
        return j.toolsize() <= capacity;
    }

    private int getProperToolIndexFromSlottobeReplacedForJob(ArrayList<Integer> slot, Job job) {
        if (slot.size() < capacity) {
            return slot.size();
        }
        for (int i = 0; i < slot.size(); i++) {
            if (job.toolCanbeReplacedFromslotForMe(slot.get(i))) {
                return i;
            }
        }
        return -1;

    }
    
    private ArrayList<Integer> getProperToolIndexFromSlottobeReplacedForJob(ArrayList<Integer> slot, int j) {
        Job job = jobs.get(j);
        ArrayList<Integer> removable = new ArrayList<>(capacity);
        for (int i = 0; i < slot.size(); i++) {
            if (job.toolCanbeReplacedFromslotForMe(slot.get(i))) {
                removable.add(i);
            }
        }
        int need[] = new int[removable.size()];
        for (int i = 0; i < removable.size(); i++) {
            need[i] = 1000;
            Integer tool = slot.get(removable.get(i));
            for (int jb = j + 1; jb < jobs.size(); jb++) {
                Job job2 = jobs.get(jb);
                if (!job2.toolCanbeReplacedFromslotForMe(tool)) {
                    need[i] = jb;
                }
            }
        }
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int i = 1; i < need.length - 1; i++) {
                for (int k = i + 1; k < need.length; k++) {
                    if (need[i] < need[k]) {
                        flag = true;
                        int temp = need[i];
                        need[i] = need[k];
                        need[k] = temp;
                        temp = removable.get(i);
                        removable.set(i, removable.get(k));
                        removable.set(k, temp);
                    }
                }
            }
        }

        return removable;

    }
    
    public void setFit() {
        for (int j = 0; j < jobs.size(); j++) {
            addJob(j);
        }
    }

    public ArrayList costofAddingJob(Job j) {
        ArrayList<Integer> newslot = slots.isEmpty() ? new ArrayList<>(capacity) : new ArrayList<>(slots.get(slots.size() - 1));
        ArrayList<Integer> requiredTools = j.requiredTools(newslot, this);
        ArrayList<Object> result = new ArrayList<>();
        result.add(newslot);
        result.add(requiredTools);
        int n_sw = Math.max(0, requiredTools.size() - capacity + getLastSlotSize());
        result.add(Integer.valueOf(n_sw));
        return result;
    }

    public void addJob(Job j) {
        ArrayList<Integer> newslot = slots.isEmpty() ? new ArrayList<>(capacity) : new ArrayList<>(slots.get(slots.size() - 1));
        ArrayList<Integer> requiredTools = j.requiredTools(newslot, this);
        addJob(j, newslot, requiredTools);
    }
    
    public void addJob(int j) {
        ArrayList<Integer> newslot = slots.isEmpty() ? new ArrayList<>(capacity) : new ArrayList<>(slots.get(slots.size() - 1));
        ArrayList<Integer> requiredTools = jobs.get(j).requiredTools(newslot, this);
        addJob(j, newslot, requiredTools);
    }

    public void addJob(Job j, ArrayList<Integer> newslot, ArrayList<Integer> requiredTools) {
        int r = requiredTools.size() > 0 ? rnd.nextInt(requiredTools.size()) : 0;
        removedTools.clear();
        for (int l = 0; l < requiredTools.size(); l++) {
            int t = (l + r) % requiredTools.size();
            int i = capacity == newslot.size() ? getProperToolIndexFromSlottobeReplacedForJob(newslot, j) : newslot.size();
            
            if (i == newslot.size()) {
                newslot.add(requiredTools.get(t));
                removedTools.add(requiredTools.get(t));
            } else {
                newslot.set(i, requiredTools.get(t));
            }
        }
        for (Integer tool : removedTools) {
            requiredTools.remove(tool);
        }
        int n_sw = slots.size() > 0 ? requiredTools.size() : 0;
        numberofswitches += n_sw;
        cumulativeswitches.add(numberofswitches);
        flowtime += j.getProcessingTime(this) + switchtime * n_sw;
        totalflowtime += flowtime;
        flowtimes.add(flowtime);
        jobs.add(j);
        slots.add(newslot);
    }
    
    public void addJob(int j, ArrayList<Integer> newslot, ArrayList<Integer> requiredTools) {
        removedTools.clear();
        ArrayList<Integer> ia = capacity - newslot.size() >= requiredTools.size() ? null : getProperToolIndexFromSlottobeReplacedForJob(newslot, j);
        int ia_index = 0;
        for (int l = 0; l < requiredTools.size(); l++) {
            int i = capacity == newslot.size() ? ia.get(ia_index++) : newslot.size();

            if (i == newslot.size()) {
                newslot.add(requiredTools.get(l));
                removedTools.add(requiredTools.get(l));
            } else {
                newslot.set(i, requiredTools.get(l));
            }
        }
        for (Integer tool : removedTools) {
            requiredTools.remove(tool);
        }
        int n_sw = slots.size() > 0 ? requiredTools.size() : 0;
        numberofswitches += n_sw;
        cumulativeswitches.add(numberofswitches);
        flowtime += jobs.get(j).getProcessingTime(this) + switchtime * n_sw;
        totalflowtime += flowtime;
        flowtimes.add(flowtime);
        slots.add(newslot);
    }

    public Machine copy(int point) {
        point = Math.min(point, jobs.size());
        Machine result = new Machine(index, capacity, switchtime, rnd);
        for (int j = 0; j < point; j++) {
            result.jobs.add(jobs.get(j));
            ArrayList<Integer> newslot = new ArrayList<>(capacity);
            int s = slots.get(j).size();
            for (int t = 0; t < s; t++) {
                newslot.add(slots.get(j).get(t).intValue());
            }
            result.slots.add(newslot);
            int fl = flowtimes.get(j).intValue();
            result.totalflowtime += fl;
            result.flowtimes.add(fl);
            result.cumulativeswitches.add(cumulativeswitches.get(j).intValue());

        }
        if (point > 0) {
            result.flowtime = result.flowtimes.get(point - 1).intValue();
            result.numberofswitches = result.cumulativeswitches.get(point - 1).intValue();
        }
        return result;
    }

    public Machine copyAll() {
        return copy(jobs.size());
    }

    public ArrayList<Integer> getLastSlot() {
        if (slots.isEmpty()) {
            return null;
        }
        return slots.get(slots.size() - 1);
    }

    public int getLastSlotSize() {
        ArrayList<Integer> lastslot = getLastSlot();
        if (lastslot == null) {
            return 0;
        }
        return lastslot.size();
    }

}
