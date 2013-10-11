package ch.cyberduck.core.transfer.download;

import ch.cyberduck.core.AbstractTestCase;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.LocalAttributes;
import ch.cyberduck.core.NullLocal;
import ch.cyberduck.core.NullSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.transfer.symlink.NullSymlinkResolver;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @version $Id$
 */
public class ResumeFilterTest extends AbstractTestCase {

    @Test
    public void testAcceptDirectory() throws Exception {
        ResumeFilter f = new ResumeFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        Path p = new Path("a", Path.DIRECTORY_TYPE) {
            @Override
            public Local getLocal() {
                return new NullLocal("d", "a");
            }
        };
        assertTrue(f.accept(p, new TransferStatus()));
    }

    @Test
    public void testAcceptExistsFalse() throws Exception {
        ResumeFilter f = new ResumeFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        Path p = new Path("a", Path.FILE_TYPE) {
            @Override
            public Local getLocal() {
                return new NullLocal("~/Downloads", "a") {
                    @Override
                    public boolean exists() {
                        return false;
                    }
                };
            }
        };
        p.attributes().setSize(2L);
        assertTrue(f.accept(p, new TransferStatus()));
    }

    @Test
    public void testPrepareFile() throws Exception {
        ResumeFilter f = new ResumeFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        Path p = new Path("a", Path.FILE_TYPE) {
            @Override
            public Local getLocal() {
                return new NullLocal("~/Downloads", "a") {
                    @Override
                    public LocalAttributes attributes() {
                        return new LocalAttributes("a") {
                            @Override
                            public long getSize() {
                                return 1L;
                            }
                        };
                    }
                };
            }
        };
        p.attributes().setSize(2L);
        final TransferStatus status = f.prepare(p, new TransferStatus());
        assertTrue(status.isAppend());
        assertEquals(1L, status.getCurrent(), 0L);
    }

    @Test
    public void testPrepareDirectoryExists() throws Exception {
        ResumeFilter f = new ResumeFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        Path p = new Path("a", Path.DIRECTORY_TYPE);
        p.setLocal(new NullLocal(null, "a"));
        final TransferStatus status = f.prepare(p, new TransferStatus().exists(true));
        assertTrue(status.isExists());
    }

    @Test
    public void testPrepareDirectoryExistsFalse() throws Exception {
        ResumeFilter f = new ResumeFilter(new NullSymlinkResolver(), new NullSession(new Host("h")));
        Path p = new Path("a", Path.DIRECTORY_TYPE);
        p.setLocal(new NullLocal(null, "a") {
            @Override
            public boolean exists() {
                return false;
            }
        });
        final TransferStatus status = f.prepare(p, new TransferStatus());
        assertFalse(status.isAppend());
    }
}
