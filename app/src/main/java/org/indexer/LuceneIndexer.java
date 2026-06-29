package org.indexer;


import org.model.ParquetRow;
//import org.model.IndexSchema;
//import org.model.IndexSchema.LuceneFieldType;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
//import java.util.List;
//import java.util.ArrayList;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field;
//import org.apache.lucene.document.FloatField;
//import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.DoubleField;

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
     * Constructeur de LuceneIndexer, instancie le writer.
     * 
     * @param indexPath Le chemin vers le fichier sur lequel écrire l'index.
     * @throws IOException
     */
    public LuceneIndexer(Path indexPath) throws IOException {
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
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) continue;

            //LuceneFieldType type = IndexSchema.arrowToLuceneField(
            //    new org.apache.arrow.vector.types.pojo.Field(
            //        key, value,
            //        new ArrayList<org.apache.arrow.vector.types.pojo.Field>()
            //    )
            //);
//
            //switch (type) {
//
            //    case LuceneFieldType.TextField:
            //        doc.add(new TextField(key, value.toString(), Field.Store.YES));
            //        break;
//
            //    case LuceneFieldType.StringField:
            //        doc.add(new StringField(key, value.toString(), Field.Store.YES));
            //        break;
//
            //    case LuceneFieldType.IntField:
            //        doc.add(new IntField(key, ((Number) value).intValue(), Field.Store.YES));
            //        break;
//
            //    case LuceneFieldType.LongField:
            //        doc.add(new LongField(key, ((Number) value).longValue(), Field.Store.YES));
            //        break;
//
            //    case LuceneFieldType.FloatField:
            //        doc.add(new FloatField(key, ((Number) value).floatValue(), Field.Store.YES));
            //        break;
//
            //    case LuceneFieldType.DoubleField:                    
            //        doc.add(new DoubleField(key, ((Number) value).doubleValue(), Field.Store.YES));
            //        break;
//
            //    default:
            //        break;
            //}

            if (value instanceof Long l) {
                doc.add(new LongField(key, l, Field.Store.YES));
            } else if (value instanceof Integer i) {
                doc.add(new DoubleField(key, i, Field.Store.YES));
            } else if (value instanceof Float f) {
                doc.add(new DoubleField(key, f, Field.Store.YES));
            } else if (value instanceof Double d) {
                 doc.add(new DoubleField(key, d, Field.Store.YES));
            } else if (value instanceof Boolean b) {
                // Lucene n'a pas de Field de bouléen
                doc.add(new StringField(key, b.toString(), Field.Store.YES));
            } else {
                String text = value.toString();
                if (isFullTextField(key)) {
                    doc.add(new TextField(key, text, Field.Store.YES));
                } else {
                    doc.add(new StringField(key, text, Field.Store.YES));
                }
            }
        }

        this.writer.addDocument(doc);
    }



    /**
     * Détermine si un champ doit être indexé en full-text (TextField)
     * ou en valeur exacte (StringField).
     * 
     * @param fieldName Le nom du champ.
     */
    private boolean isFullTextField(String fieldName) {
        return switch (fieldName) {
            case "description", "titre", "contenue", "texte" -> true;
            default -> false;
        };
    }



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