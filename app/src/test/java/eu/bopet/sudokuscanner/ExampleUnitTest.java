package eu.bopet.sudokuscanner;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void solverTest() {
        int[][] numbers = {
                {0, 5, 8, 0, 3, 0, 0, 2, 0},
                {4, 0, 2, 0, 0, 0, 9, 0, 5},
                {0, 0, 7, 0, 0, 0, 6, 8, 0},
                {2, 9, 0, 0, 5, 4, 0, 7, 0},
                {5, 0, 0, 0, 6, 2, 0, 0, 0},
                {0, 0, 3, 8, 1, 0, 2, 5, 0},
                {1, 0, 9, 0, 0, 3, 0, 6, 4},
                {8, 6, 5, 4, 9, 0, 1, 3, 0},
                {0, 7, 0, 0, 0, 6, 0, 0, 0}
        };
        int [][] solution = Solver.solve(numbers,3);
        assert solution.length>0;
        for (int i = 0; i<solution.length;i++){
            for (int j = 0; j<solution.length;j++){
                System.out.print(solution[i][j] + ", ");
                assert solution[i][j]!=0;
            }
            System.out.println("");
        }
    }
}