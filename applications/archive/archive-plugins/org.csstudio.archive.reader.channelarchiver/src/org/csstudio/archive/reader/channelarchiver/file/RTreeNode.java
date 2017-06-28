/*******************************************************************************
 * Copyright (c) 2017 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.reader.channelarchiver.file;

import java.time.Instant;

/** One node in the RTree
 *
 *  @author Kay Kasemir
 *  @author Amanda Carpenter
 */
public class RTreeNode
{
    static class Record
    {
        public final Instant start, end;
        public final long child;

        public Record(final ArchiveFileBuffer buffer) throws Exception
        {
            // An RTree Node Record is 20 bytes, arranged as follows:
            // EpicsTime start time, where an EpicsTime is 8 bytes long
            // EpicsTime end time
            // long child - if empty, 0;
            //              if Node is not leaf, offset of child node;
            //              if Node is leaf, offset of child Datablock
            start = buffer.getEpicsTime();
            end = buffer.getEpicsTime();
            child = buffer.getUnsignedInt();
        }
    };

    public final long offset;
    public final int M;
    public final boolean isLeaf;
    public final long parent;
    public final Record[] records;

    public RTreeNode(final ArchiveFileBuffer buffer, final long offset, final int M) throws Exception
    {
        // An RTree Node is laid out as follows:
        // byte isLeaf (if false, 0; otherwise, true)
        // long parent (if root, 0; otherwise, offset of parent node)
        // Record[M] records, where a Record is 20 bytes
        this.offset = offset;
        this.M = M;
        buffer.offset(offset);
        isLeaf = buffer.get() != 0;
        parent = buffer.getUnsignedInt();
        records = new Record[M];
        for (int i=0; i<M; ++i)
            records[i] = new Record(buffer);
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder();
        buf.append("RTree Node @ 0x" + Long.toHexString(offset));
        if (isLeaf)
            buf.append(" (leaf)");
        if (parent != 0)
            buf.append(" parent @ 0x" + Long.toHexString(parent));
        buf.append('\n');
        for (int i=0; i<M; ++i)
        {

            buf.append('[').append(i).append("] ")
               .append(ArchiveFileTime.format(records[i].start))
               .append(" - ")
               .append(ArchiveFileTime.format(records[i].end))
               .append(" -> 0x")
               .append(Long.toHexString(records[i].child))
               .append('\n');
        }
        return buf.toString();
    }
}
