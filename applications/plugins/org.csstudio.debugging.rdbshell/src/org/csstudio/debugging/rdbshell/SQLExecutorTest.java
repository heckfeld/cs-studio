package org.csstudio.debugging.rdbshell;

import java.util.ArrayList;

import org.junit.Test;

/** JUnit Test of SQLExecutor
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class SQLExecutorTest
{
    @Test
    public void testRDB() throws Exception
    {
        final SQLExecutor rdb =
            new SQLExecutor("jdbc:oracle:thin:@//172.31.73.122:1521/prod", "chan_arch", "sns");
        
        // final ArrayList<String[]> rows = rdb.execute("SELECT * FROM chan_arch.smpl_eng ORDER BY url");
        final ArrayList<String[]> rows = rdb.execute("select * from chan_arch.chan_grp where eng_id=2");
        for (String[] row : rows)
        {
            for (int i = 0; i < row.length; i++)
            {
                final String col = row[i];
                if (i > 0)
                    System.out.print(", ");
                System.out.print(col);
            }
            System.out.print("\n");
        }
    }
}
