package com.thinkbiganalytics.file.parsers.csv;

/*-
 * #%L
 * kylo-file-metadata-util
 * %%
 * Copyright (C) 2017 ThinkBig Analytics
 * %%
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
 * #L%
 */


import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Infers CSV format for a sample file
 */

public class CSVAutoDetect {

    private static final Logger LOG = LoggerFactory.getLogger(CSVAutoDetect.class);

    /*
    private static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {

        return map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

*/

    private CSVRecord firstRecord;

    public interface Predicate<T> {
        boolean apply(T type);
    }


    private static <K, V extends Comparable<? super V>> Map<K, V> sortMapByValue(Map<K, V> map) {
        List<Map.Entry<K,V>> list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                    .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        Map<K,V> sortedHashMap = new LinkedHashMap();
        for(Map.Entry<K,V> item: list) {
            sortedHashMap.put(item.getKey(), item.getValue());
        }
        return sortedHashMap;
    }


    private <E> boolean allMatch(List<E> list, Predicate<E> predicate){
        boolean allMatch = false;
        for(E e:list){
            if(predicate.apply(e)){
                allMatch = true;
            }
            else {
                allMatch = false;
                break;
            }
        }
        return allMatch;
    }

    private <E> boolean anyMatch(List<E> list, Predicate<E> predicate){
        boolean match = false;
        for(E e:list){
            if(predicate.apply(e)){
                match = true;
                break;
            }

        }
        return match;
    }

    public class HasQuotePredicate implements Predicate<LineStats> {
        private Character quoteChar;
        public HasQuotePredicate(Character quoteChar){
            this.quoteChar = quoteChar;
        }
        @Override
        public boolean apply(LineStats type) {
            return type.hasLegalQuotedStringOfChar(this.quoteChar);
        }
    }

    public class ContainsNoDelimCharOfTypePredicate implements Predicate<LineStats> {
        private Character quoteChar;
        public ContainsNoDelimCharOfTypePredicate(Character quoteChar){
            this.quoteChar = quoteChar;
        }
        @Override
        public boolean apply(LineStats type) {
            return type.containsNoDelimCharOfType(this.quoteChar);
        }
    }


    public class CSVRecordSizePredicate implements Predicate<CSVRecord> {
        private int size;
        public CSVRecordSizePredicate(int size){
            this.size =size;
        }

        @Override
        public boolean apply(CSVRecord record) {
            return record.size() == size;
        }
    }

    public  List<String> getHeader(){
        List<String> headers = new LinkedList<>();
        if(firstRecord != null){
            for(int i =0; i<firstRecord.size();i++){
                headers.add(firstRecord.get(i));
            }
        }
        return headers;
    }


    /**
     * Parses a sample file to allow schema specification when creating a new feed.
     *
     * @param sampleText the sample text
     * @return A configured parser
     * @throws IOException If there is an error parsing the sample file
     */
    public CSVFormat detectCSVFormat(String sampleText, boolean headerRow, String seperatorStr) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.withAllowMissingColumnNames();
        Character separatorChar = null;
        if (StringUtils.isNotBlank(seperatorStr)) {
            separatorChar = seperatorStr.charAt(0);
        }
        try (BufferedReader br = new BufferedReader(new StringReader(sampleText))) {
            List<LineStats> lineStats = generateStats(br, separatorChar);
            Character quote = guessQuote(lineStats);
            Character delim = guessDelimiter(lineStats, sampleText, quote, headerRow);
            if (delim == null) {
                throw new IOException("Unrecognized format");
            }
            format = format.withDelimiter(delim);
            format = format.withQuoteMode(QuoteMode.MINIMAL).withQuote(quote);
        }
        return format;
    }

    private List<LineStats> generateStats(BufferedReader br, Character separatorChar) throws IOException {
        List<LineStats> lineStats = new Vector<>();
        String line;
        int rows = 0;
        while ((line = br.readLine()) != null && rows < 100) {
            LineStats stats = new LineStats(line, separatorChar);
            rows++;
            lineStats.add(stats);
        }
        return lineStats;
    }

    private boolean hasEscapes(List<LineStats> lineStats) {
        for (LineStats lineStat : lineStats) {
            if (lineStat.escapes) {
                return true;
            }
        }
        return false;
    }

    private Character guessQuote(List<LineStats> lineStats) {
        Character[] quoteTypeSupported = {Character.valueOf('"'), Character.valueOf('\'')};
        boolean match = false;
        for (Character quoteType : quoteTypeSupported) {
            boolean quoteTypeFound = anyMatch(lineStats,new ContainsNoDelimCharOfTypePredicate(quoteType));
            if (quoteTypeFound) {
                match = allMatch(lineStats,new HasQuotePredicate(quoteType));
            }
            if (match) {
                return quoteType;
            }
        }
        return CSVFormat.DEFAULT.getQuoteCharacter();
    }

    private Character guessDelimiter(List<LineStats> lineStats, String value, Character quote, boolean headerRow) throws IOException {

        // Assume delimiter exists in first line and compare to subsequent lines
        if (lineStats.size() > 0) {
            LineStats firstLineStat = lineStats.get(0);
            Map<Character, Integer> firstLineDelimCounts = firstLineStat.calcDelimCountsOrdered();
            if (firstLineDelimCounts != null && firstLineDelimCounts.size() > 0) {
                List<Character> candidates = new ArrayList<>();
                // Attempt to parse given delimiter
                Set<Character> firstLineDelimKeys = sortDelimitersIntoPreferredOrder(firstLineDelimCounts.keySet());
                for (Character delim : firstLineDelimKeys) {
                    CSVFormat format;
                    if (headerRow) {
                        format = CSVFormat.DEFAULT.withFirstRecordAsHeader().withDelimiter(delim).withQuote(quote);
                    } else {
                        format = CSVFormat.DEFAULT.withDelimiter(delim).withQuote(quote);
                    }
                    try (StringReader sr = new StringReader(value)) {
                        try (CSVParser parser = format.parse(sr)) {
                            if (parser.getHeaderMap() != null) {
                                int size = parser.getHeaderMap().size();
                                List<CSVRecord> records = parser.getRecords();
                                if(records != null) {
                                    firstRecord = records.get(0);
                                }
                                boolean match = allMatch(records, new CSVRecordSizePredicate(size));
                                if (match) {
                                    return delim;
                                }
                            }
                        }
                    }
                    Integer delimCount = firstLineDelimCounts.get(delim);
                    boolean match = true;
                    for (int i = 1; i < lineStats.size() && match; i++) {
                        LineStats thisLine = lineStats.get(i);
                        Integer rowDelimCount = thisLine.delimStats.get(delim);
                        match = delimCount.equals(rowDelimCount);
                    }
                    if (match) {
                        candidates.add(delim);
                    }
                }
                if (candidates.size() > 0) {
                    // All agree on a single delimiter
                    if (candidates.size() == 1) {
                        return candidates.get(0);
                    } else {
                        int count = 0;
                        // Return highest delimiter from candidates
                        for (Character delim : firstLineDelimKeys) {
                            if (candidates.get(count++) != null) {
                                return delim;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    final private static String delimiterPreference = ",\t;:|~+ ";

    /**
     * Using data-gov as a guide for choice in delimiter preference, this method will take
     * a set of delimiters and sort them into a preferred order for CSV parsing.
     *
     * @implNote see https://data-gov.tw.rpi.edu/wiki/CSV_files_use_delimiters_other_than_commas
     * @param delimiters
     */
    private SortedSet<Character> sortDelimitersIntoPreferredOrder(Set<Character> delimiters) {
        SortedSet<Character> sortedDelimiters = new TreeSet<>(
            new Comparator<Character>() {
                @Override
                public int compare(Character o1, Character o2) {
                    int o1Idx = delimiterPreference.indexOf(o1);
                    int o1Pos = (o1Idx == -1)?(delimiterPreference.length() + o1):(o1Idx);
                    int o2Idx = delimiterPreference.indexOf(o2);
                    int o2Pos = (o2Idx == -1)?(delimiterPreference.length() + o2):(o2Idx);
                    return o1Pos - o2Pos;
                }
            });


        sortedDelimiters.addAll(delimiters);
        return sortedDelimiters;
    }

    static class LineStats {

        Map<Character, Integer> delimStats = new HashMap<Character, Integer>();
        Map<Character, Integer> nonDelimStats = new HashMap<Character, Integer>();
        Character lastChar;
        boolean escapes;
        Character firstDelim;


        /**
         * @param line              the line to evaluate
         * @param suppliedDelimiter the explicit delimiter supplied by the user, or null if not defined
         */
        public LineStats(String line, final Character suppliedDelimiter) {
            // Look for delimiters
            char[] chars = line.toCharArray();
            String delimiterString = " :;|\t+~";
            String nonDelimiterString = "\"'<>\\";

            if (suppliedDelimiter != null) {
                delimiterString += Character.toString(suppliedDelimiter);
            }

            char[] delimiters = delimiterString.toCharArray();
            char[] nonDelimiters = nonDelimiterString.toCharArray();

            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];

                boolean found = false;
                for (char delim : delimiters) {
                    if (c == delim) {
                        increment(delim, true);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    for (char delim : nonDelimiters) {
                        if (c == delim) {
                            increment(delim, false);
                            break;
                        }
                    }
                }
            }
        }

        boolean containsNoDelimCharOfType(Character quoteType) {
            Integer count = nonDelimStats.get(quoteType);
            return (count != null);
        }

        boolean hasLegalQuotedStringOfChar(Character quoteType) {
            Integer count = nonDelimStats.get(quoteType);
            return (count == null || count.intValue() % 2 == 0);
        }

        private void increment(Character c, boolean delim) {
            if (delim) {
                if (lastChar == null || (lastChar != '\\') || (lastChar == '\\' && c == '\\')) {
                    Integer val = delimStats.get(c);
                    val = (val == null ? 1 : val.intValue() + 1);
                    if (val == 1) {
                        firstDelim = c;
                    }
                    delimStats.put(c, val);
                } else {
                    escapes = true;
                }
            } else {
                Integer val = nonDelimStats.get(c);
                val = (val == null ? 1 : val.intValue() + 1);
                nonDelimStats.put(c, val);
            }
            lastChar = c;
        }

        public Map<Character, Integer> calcDelimCountsOrdered() {
            return sortMapByValue(delimStats);
        }

    }
}
