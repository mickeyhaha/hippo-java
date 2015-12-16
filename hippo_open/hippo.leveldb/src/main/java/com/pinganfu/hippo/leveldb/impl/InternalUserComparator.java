/**
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinganfu.hippo.leveldb.impl;

import com.google.common.base.Preconditions;
import com.pinganfu.hippo.leveldb.table.UserComparator;
import com.pinganfu.hippo.leveldb.util.Slice;

import static com.pinganfu.hippo.leveldb.impl.SequenceNumber.MAX_SEQUENCE_NUMBER;
public class InternalUserComparator implements UserComparator
{
    private final InternalKeyComparator internalKeyComparator;

    public InternalUserComparator(InternalKeyComparator internalKeyComparator)
    {
        this.internalKeyComparator = internalKeyComparator;
    }

    @Override
    public int compare(Slice left, Slice right)
    {
        return internalKeyComparator.compare(new InternalKey(left), new InternalKey(right));
    }

    @Override
    public String name() {
        return internalKeyComparator.name();
    }

    @Override
    public Slice findShortestSeparator(
            Slice start,
            Slice limit)
    {
        /*
         * 剔除过期时间 20150819
         */
        // Attempt to shorten the user portion of the key
        InternalKey startIkey = new InternalKey(start);
        InternalKey limitIkey = new InternalKey(limit);
        if (startIkey.original()) {
            startIkey = startIkey.simplify();
        }
        if (limitIkey.original()) {
          limitIkey = limitIkey.simplify();
        }
        Slice startUserKey = startIkey.getUserKey();
        Slice limitUserKey = limitIkey.getUserKey();
        
        Slice shortestSeparator = internalKeyComparator.getUserComparator().findShortestSeparator(startUserKey, limitUserKey);

        if (internalKeyComparator.getUserComparator().compare(startUserKey, shortestSeparator) < 0) {
            // User key has become larger.  Tack on the earliest possible
            // number to the shortened user key.
            InternalKey newInternalKey = new InternalKey(shortestSeparator, MAX_SEQUENCE_NUMBER, ValueType.VALUE);
            Preconditions.checkState(compare(startIkey.encode(), newInternalKey.encode()) < 0);// todo
            Preconditions.checkState(compare(newInternalKey.encode(), limitIkey.encode()) < 0);// todo

            return newInternalKey.encode();
        }

        return start;
    }

    @Override
    public Slice findShortSuccessor(Slice key)
    {
        /*
         * 剔除过期时间 20150819
         */
        InternalKey ikey = new InternalKey(key);
        if (ikey.original()) {
          ikey = ikey.simplify();
        }
        Slice userKey = ikey.getUserKey();
        Slice shortSuccessor = internalKeyComparator.getUserComparator().findShortSuccessor(userKey);

        if (internalKeyComparator.getUserComparator().compare(userKey, shortSuccessor) < 0) {
            // User key has become larger.  Tack on the earliest possible
            // number to the shortened user key.
            InternalKey newInternalKey = new InternalKey(shortSuccessor, MAX_SEQUENCE_NUMBER, ValueType.VALUE);
            Preconditions.checkState(compare(ikey.encode(), newInternalKey.encode()) < 0);// todo

            return newInternalKey.encode();
        }

        return key;
    }
}