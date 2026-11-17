package com.csv.csvimport.csv;

import java.nio.file.Path;
import java.util.Iterator;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

public class CsvItemReader implements ItemStreamReader<CSVRecord> {
    private final CsvFileUtils files;
    private final Path path;
    private final String operation;
    private CSVParser parser;
    private Iterator<CSVRecord> records;

    public CsvItemReader(CsvFileUtils files, String filename, String operation) {
        this.files = files;
        this.path = Path.of(filename);
        this.operation = operation;
    }

    @Override
    public void open(ExecutionContext context) {
        try {
            parser = files.open(path, operation);
            records = parser.iterator();
        } catch (Exception e) {
            throw new IllegalArgumentException("CSV-Datei konnte nicht gelesen werden", e);
        }
    }

    @Override
    public CSVRecord read() { return records.hasNext() ? records.next() : null; }

    @Override
    public void close() {
        if (parser != null) {
            try { parser.close(); }
            catch (Exception e) { throw new IllegalStateException("CSV-Datei konnte nicht geschlossen werden", e); }
        }
    }
}


