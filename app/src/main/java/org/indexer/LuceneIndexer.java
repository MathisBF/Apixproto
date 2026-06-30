package org.indexer;


import org.model.IndexSchema;
import org.model.ParquetRow;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;


/**
 * La classe LuceneIndexer permet de créer un index Lucene à partir de données Parquet via ParquetRow.
 */
public class LuceneIndexer implements AutoCloseable {

    /**
     * Le writer, écrit l'index.
     */
    private final IndexWriter writer;
    /**
     * Le schéma de l'index.
     */
    private final IndexSchema schema;
    /**
     * Le chemin vers l'index.
     */
    private final Path indexPath;



    /**
     * Constructeur de LuceneIndexer, instancie le writer.
     * 
     * @param indexPath Le chemin vers le fichier sur lequel écrire l'index.
     * @throws IOException
     */
    public LuceneIndexer(Path indexPath, IndexSchema schema) throws IOException {
        
        this.indexPath = indexPath;
        this.schema = schema;

        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // CREATE_OR_APPEND pour ajouter si l'index existe déjà
        this.writer = new IndexWriter(directory, config);
    }



    /**
     * Indexe un objet ParquetRow, soit une ligne d'un fichier parquet.
     * 
     * @param row La ligne à indexer.
     * @throws IOException
     */
    public void writeIndex(ParquetRow row) throws IOException {
        
        Document doc = new Document();

        for (Map.Entry<String, Object> entry : row.getFields().entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            if (value == null) continue;

            this.schema.addField(doc, fieldName, value);
        }

        this.writer.addDocument(doc);
    }



    /**
     * Commit les ajouts du writer sur l'index.
     * 
     * @throws IOException
     */
    public void commit() throws IOException {
        
        this.schema.save(this.indexPath);
        this.writer.commit();
        System.out.println("Index créé : " + writer.getDocStats().numDocs + " documents indexés.");
    }



    /**
     * @return Le chemin vers l'index.
     */
    public Path getPath() {
        return this.indexPath;
    }



    /**
     * Ferme le writer.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        this.writer.close();
    }
}