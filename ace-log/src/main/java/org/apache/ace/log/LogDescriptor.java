/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ace.log;

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.ace.range.SortedRangeSet;
import org.apache.ace.util.Codec;

/**
 * Instances of this class represent a range of log events. Such a range is defined by:
 * <ul>
 * <li>A unique gateway ID</li>
 * <li>A log ID unique for this gateway</li>
 * <li>A set of event IDs</li>
 * </ul>
 */
public class LogDescriptor {
    private final SortedRangeSet m_rangeSet;
    private final long m_logID;
    private final String m_gatewayID;

    /**
     * Create a log event range.
     *
     * @param gatewayID The unique gateway ID
     * @param logID The unique log ID for this gateway
     * @param rangeSet The set of event IDs
     */
    public LogDescriptor(String gatewayID, long logID, SortedRangeSet rangeSet) {
        m_gatewayID = gatewayID;
        m_logID = logID;
        m_rangeSet = rangeSet;
    }

    /**
     * Create a log event range from a string representation. String representations
     * should be formatted as "gatewayID,logID,eventIDs" where each substring is formatted
     * using <code>Codec.encode(string)</code> method.
     *
     * Throws an <code>IllegalArgumentException</code> when the string representation is not correctly formatted.
     *
     * @param representation String representation of the log event range
     */
    public LogDescriptor(String representation) {
        try {
            StringTokenizer st = new StringTokenizer(representation, ",");
            m_gatewayID = Codec.decode(st.nextToken());
            m_logID = Long.parseLong(st.nextToken());
            String rangeSet = "";
            if (st.hasMoreTokens()) {
                rangeSet = st.nextToken();
            }
            m_rangeSet = new SortedRangeSet(Codec.decode(rangeSet));
        }
        catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Could not create range from: " + representation);
        }
    }

    /**
     * Get the unique gateway identifier.
     *
     * @return Unique gateway identifier.
     */
    public String getTargetID() {
        return m_gatewayID;
    }

    /**
     * Get the unique log identifier for this gateway.
     *
     * @return Unique log identifier for this gateway.
     */
    public long getLogID() {
        return m_logID;
    }

    /**
     * Get the range set of the log event range.
     *
     * @return The range set
     */
    public SortedRangeSet getRangeSet() {
        return m_rangeSet;
    }

    /**
     * Get a string representation of the log event range. String representations
     * generated by this method can be used to construct new <code>LogDescriptor</code> instances.
     *
     * @return A string representation of the log event range
     */
    public String toRepresentation() {
        StringBuffer result = new StringBuffer();
        result.append(Codec.encode(m_gatewayID));
        result.append(',');
        result.append(m_logID);
        result.append(',');
        result.append(Codec.encode(m_rangeSet.toRepresentation()));
        return result.toString();
    }
}