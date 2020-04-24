package eu.bopet.sudokuscanner;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    public static int[][] solve(int[][] numbers, int size) {
        int[][] solution = new int[numbers.length][numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            for (int j = 0; j < numbers.length; j++) {
                solution[i][j] = numbers[i][j];
            }
        }
        int nol = 0;
        int oldNol = numbers.length * numbers.length;
        boolean iterate = true;
        List<Integer>[][] pn = new ArrayList[solution.length][solution.length];
        int bx, by;

        for (int i = 0; i < solution.length; i++) {
            for (int j = 0; j < solution.length; j++) {
                if (solution[i][j] == 0) {
                    pn[i][j] = new ArrayList<>();
                    for (int k = 1; k < solution.length + 1; k++) {
                        pn[i][j].add(k);
                    }
                }
            }
        }
        while (iterate) {
            for (int i = 0; i < solution.length; i++) {
                for (int j = 0; j < solution.length; j++) {
                    if (solution[i][j] == 0) {
                        //check column
                        for (int k = 0; k < solution.length; k++) {
                            if (solution[i][k] > 0 && pn[i][j].contains(solution[i][k])) {
                                pn[i][j].remove((Integer) solution[i][k]);
                            }
                        }
                        //check row
                        for (int k = 0; k < solution.length; k++) {
                            if (solution[k][j] > 0 && pn[i][j].contains(solution[k][j])) {
                                pn[i][j].remove((Integer) solution[k][j]);
                            }
                        }
                        //check block
                        for (int k = 0; k < size; k++) {
                            for (int l = 0; l < size; l++) {
                                bx = (i / size + k);
                                by = (j / size + l);
                                if (solution[i][j] > 0 && pn[bx][by].contains(solution[i][j])) {
                                    pn[bx][by].remove((Integer) solution[i][j]);
                                }
                            }
                        }
                    }
                }
            }

            //check block again
            List<Integer> unique;
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    for (int k = 0; k < size; k++) {
                        for (int l = 0; l < size; l++) {
                            int ci = i * size + k;
                            int cj = j * size + l;
                            if (pn[ci][cj] != null && pn[ci][cj].size() > 1) {
                                unique = new ArrayList<>();
                                unique.addAll(pn[ci][cj]);
                                unique.remove((Integer) 0);
                                for (int nk = 0; nk < size; nk++) {
                                    for (int nl = 0; nl < size; nl++) {
                                        if (nk == k && nl == l) {
                                        } else {
                                            int nci = i * size + nk;
                                            int ncj = j * size + nl;
                                            if (unique.contains((Integer)solution[nci][ncj])){
                                                unique.remove((Integer) solution[nci][ncj]);
                                            }
                                            List<Integer> cnu = pn[nci][ncj];
                                            if (cnu != null){
                                                unique.removeAll(cnu);
                                            }
                                        }
                                    }
                                }
                                if (unique.size()==1){
                                    pn[ci][cj].clear();
                                    pn[ci][cj].add(unique.get(0));
                                }
                            }
                        }
                    }
                }
            }

            //check for improvements
            for (int i = 0; i < solution.length; i++) {
                for (int j = 0; j < solution.length; j++) {
                    if (pn[i][j] != null && pn[i][j].size() == 1) {
                        solution[i][j] = pn[i][j].get(0);
                        pn[i][j].clear();
                    } else {
                        nol++;
                    }
                }
            }
            if (oldNol > nol) {
                iterate = true;
                oldNol = nol;
                nol = 0;
            } else {
                iterate = false;
            }
        }
        return solution;
    }
}
