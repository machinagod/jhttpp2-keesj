/* Written and copyright 2001 Benjamin Kohl.
 * Distributed under the GNU General Public License; see the README file.
 * This code comes with NO WARRANTY.
 */

import Jhttpp2HTTPSession;
import java.io.IOException;

public interface Jhttpp2InputStream
{
  /** reads the data */
  public int read_f(byte[] b) throws IOException;
}
