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
    private final IndexSchema schema;



    /**
     * Constructeur de LuceneIndexer, instancie le writer.
     * 
     * @param indexPath Le chemin vers le fichier sur lequel écrire l'index.
     * @throws IOException
     */
    public LuceneIndexer(Path indexPath, IndexSchema schema) throws IOException {
        Directory directory = FSDirectory.open(indexPath);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE); // CREATE_OR_APPEND pour ajouter si l'index existe déjà
        this.writer = new IndexWriter(directory, config);
        this.schema = schema;
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

            //if (value instanceof Long l) {
            //    doc.add(new LongField(fieldName, l, Field.Store.YES));
            //} else if (value instanceof Integer i) {
            //    doc.add(new DoubleField(fieldName, i, Field.Store.YES));
            //} else if (value instanceof Float f) {
            //    doc.add(new DoubleField(fieldName, f, Field.Store.YES));
            //} else if (value instanceof Double d) {
            //     doc.add(new DoubleField(fieldName, d, Field.Store.YES));
            //} else if (value instanceof Boolean b) {
            //    // Lucene n'a pas de Field de bouléen
            //    doc.add(new StringField(fieldName, b.toString(), Field.Store.YES));
            //} else {
            //    String text = value.toString();
            //    if (isFullTextField(fieldName)) {
            //        doc.add(new TextField(fieldName, text, Field.Store.YES));
            //    } else {
            //        doc.add(new StringField(fieldName, text, Field.Store.YES));
            //    }
            //}
        }

        this.writer.addDocument(doc);
    }



    ///**
    // * Détermine si un champ doit être indexé en full-text (TextField)
    // * ou en valeur exacte (StringField).
    // * 
    // * @param fieldName Le nom du champ.
    // */
    //private boolean isFullTextField(String fieldName) {
    //    return switch (fieldName) {
    //        case "description", "titre", "contenue", "texte" -> true;
    //        default -> false;
    //    };
    //}



    /**
     * Commit les ajouts du writer sur l'index.
     * 
     * @throws IOException
     */
    public void commit() throws IOException {
        this.writer.commit();
        System.out.println("Index créé : " + writer.getDocStats().numDocs + " documents indexés.");
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