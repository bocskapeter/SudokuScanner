package eu.bopet.sudokuscanner;

import java.util.ArrayList;
import java.util.List;

public class Solver {

    public static int[][] solve(int[][]numbers,int size){
        int[][] solution = new int[numbers.length][numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            for (int j = 0; j < numbers.length; j++) {
                solution[i][j] = numbers[i][j];
            }
        }
        int nol = 0;
        int oldNol = numbers.length * numbers.length;
        boolean converged = true;
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
        while (converged) {
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
                        if (pn[i][j].size() == 1) {
                            solution[i][j] = pn[i][j].get(0);
                            pn[i][j] = null;
                        } else {
                            nol++;
                        }
                    }
                }
            }
            if (oldNol > nol) {
                converged = true;
                oldNol = nol;
                nol = 0;
            } else {
                converged = false;
            }
        }
        return solution;
    }
}
