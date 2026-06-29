package org.searcher;


import org.model.IndexSchema;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import org.apache.lucene.store.FSDirectory;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.LongField;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;



/**
 * La classe LuceneSearcher permet d'effectuer des requêtes via Lucene sur un index déjà existant.
 */
public class LuceneSearcher implements AutoCloseable {

    /**
     * Le searcher, permet de lancer les requêtes et de lire leurs résultats.
     */
    private IndexSearcher searcher;
    /**
     * L'analyzer, permet d'interpréter des requêtes sous forme de textes.
     */
    private final StandardAnalyzer analyzer;
    /**
     * Le schema des données indexés.
     */
    private IndexSchema schema;



    /**
     * Constructeur de LuceneSearcher, instancie les variables searcher et analyzer.
     * 
     * @param indexPath Le chemin vers le fichier contenant l'index Lucene.
     * @throws IOException
     * @throws Exception
     */
    public LuceneSearcher(Path indexPath) throws IOException, Exception {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath));
        this.searcher = new IndexSearcher(reader);
        this.analyzer = new StandardAnalyzer();
        this.schema = new IndexSchema(indexPath);
    }



    /**
     * Teste la présence ou non d'un index préexistant.
     * 
     * @param indexPath Le chemin vers l'index.
     * @return True si un index existe déjà false sinon.
     * @throws IOException
     */
    public static boolean indexExists(Path indexPath) throws IOException {
        return DirectoryReader.indexExists(FSDirectory.open(indexPath));
    }



    /**
     * Exécute une requête et affiche ses résultats à partir d'un objet Query. 
     * 
     * @param query La requête à exécuter.
     * @param label La requête sous forme de chaîne de caractère pour l'affichage.
     * @throws IOException
     */
    private void runQuery(Query query, String label) throws IOException {
        TopDocs results = this.searcher.search(query, 10);
        System.out.println("\n=== Recherche : " + label + " ===");
        System.out.println("    " + results.totalHits + " résultat(s)");
        
        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = this.searcher.storedFields().document(scoreDoc.doc);
            System.out.println("\nSCORE : " + scoreDoc.score);
            for (IndexableField field : doc.getFields()) {
                System.out.println(field.name() + " = " + field.stringValue());
            }
        }
    }



    /**
     * Affiche les résultats d'une requête.
     * 
     * @param results Les résultats de la requête.
     * @throws IOException
     */
    public void printResults(TopDocs results) throws IOException {
        System.out.println("\n\n" + results.totalHits + " résultat(s)");

        for (ScoreDoc scoreDoc : results.scoreDocs) {
            Document doc = this.searcher.storedFields().document(scoreDoc.doc);
            System.out.println("\nSCORE : " + scoreDoc.score);
            for (IndexableField field : doc.getFields()) {
                System.out.println(field.name() + " = " + field.stringValue());
            }
        }
    }



    /**
     * Formalise une requête sur un champ textuel, l'exécute et renvoie ses résultats.
     * Utilise QueryParser pour construire la requête à partir d'une chaîne de caractère.
     *  
     * @param field Le champ sur lequel la requête s'effectue.
     * @param text L'objet de la requête.
     * @param nbResults Le nombre maximum de résultats à retourner.
     * @return Les résultats de la requête.
     * @throws ParseException
     * @throws IOException
     */
    public TopDocs searchText(String field, String text, int nbResults) throws ParseException, IOException {
        Query query = new QueryParser(field, analyzer).parse(text);
        return this.searcher.search(query, nbResults);
    }



    /**
     * Formalise une requête textuel sur tous les champs de l'index, l'exécute et renvoie ses résultats.
     * Utilise MultiFieldsQueryParser pour construire la requête à partir d'une chaîne de caractère.
     * 
     * @param text L'objet de la requête.
     * @param nbResults Le nombre maximum de résultats à retourner.
     * @return Les résultats de la requête.
     * @throws IOException 
     * @throws ParseException 
     */
    public TopDocs allFieldsSearch(String text, int nbResults) throws IOException, ParseException {
        String[] fields = (String[]) this.schema.getFieldsName().toArray();        
        Query query = new MultiFieldQueryParser(fields, analyzer).parse(text);
        return this.searcher.search(query, nbResults);
    }



    /**
     * Formalise une requête textuel sur une liste de champs de l'index, l'exécute et renvoie ses résultats.
     * Utilise MultiFieldsQueryParser pour construire la requête à partir d'une chaîne de caractère.
     * 
     * @param fields La liste de champs.
     * @param text L'objet de la requête.
     * @param nbResults Le nombre maximum de résultats à retourner.
     * @return Les résultats de la requête.
     * @throws IOException 
     * @throws ParseException 
     */
    public TopDocs multiFieldsSearch(String[] fields, String text, int nbResults) throws IOException, ParseException {
        Query query = new MultiFieldQueryParser(fields, analyzer).parse(text);
        return this.searcher.search(query, nbResults);
    }



    /**
     * Formalise une recherche sur un intervalle de valeurs, l'exécute et renvoie ses résultats.
     * 
     * @param field Le champ sur lequel la requête s'effectue.
     * @param min Le seuil bas de l'intervalle.
     * @param max Le seuil haut de l'intervalle.
     * @param nbResults Le nombre maximum de résultats à retourner.
     * @return
     */
    public TopDocs searchRange(String field, long min, long max, int nbResults) {
        
        return null;
    }



    /**
     * Actualise le searcher sur l'index.
     * 
     * @throws IOException
     */
    public void refresh() throws IOException {

        DirectoryReader oldReader = (DirectoryReader) searcher.getIndexReader();
        DirectoryReader newReader = DirectoryReader.openIfChanged(oldReader);

        if (newReader != null) {
            oldReader.close();
            this.searcher = new IndexSearcher(newReader);
        }
    }



    /**
     * Permet de fermer les deux canaux de la classe, le reader du searcher et l'analyzer.
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        searcher.getIndexReader().close();
        analyzer.close();
    }



    /**
     * Formalise, exécute une requête sur un token précis et affiche ses résulats.
     * 
     * @param field Le champ sur lequel la requête s'effectue.
     * @param value Le token recherché.
     * @throws IOException
     */
    public void searchExact_Old(String field, String value) throws IOException {
        Query query = new TermQuery(new Term(field, value));
        runQuery(query, field + " = \"" + value + "\"");
    }



    /**
     * Formalise une requête sur un champ textuel, l'exécute et renvoie ses résultats.
     * Utilise QueryParser pour construire la requête à partir d'une chaîne de caractère.
     * 
     * @param field Le champ sur lequel la requête s'effectue.
     * @param text L'objet de la requête.
     * @throws ParseException
     * @throws IOException
     */
    public void searchText_Old(String field, String text) throws ParseException, IOException{
        Query query = new QueryParser(field, analyzer).parse(text);
        runQuery(query, "fulltext " + field + " = \"" + text + "\"");
    }



    /**
     * Formalise une requête sur un intervalle de Long, l'exécute et renvoie ses résultats.
     * 
     * @param field Le champ sur lequel la requête s'effectue.
     * @param min Le seuil bas de l'intervalle.
     * @param max Le seuil haut de l'intervalle.
     * @throws IOException
     */
    public void searchLongRange_Old(String field, long min, long max) throws IOException {
        Query query = LongField.newRangeQuery(field, min, max);
        runQuery(query, field + " entre " + min + " et " + max);
    }



    /**
     * Formalise une requête sur un intervalle de Double, l'exécute et renvoie ses résultats.
     * 
     * @param field Le champ sur lequel la requête s'effectue.
     * @param min Le seuil bas de l'intervalle.
     * @param max Le seuil haut de l'intervalle.
     * @throws Exception
     */
    public void searchDoubleRange_Old(String field, double min, double max) throws Exception {
        Query query = DoubleField.newRangeQuery(field, min, max);
        runQuery(query, field + " entre " + min + " et " + max);
    }



    /**
     * Formalise une requête composé de deux requêtes sur un token précis chacune, l'exécute et renvoie ses résultats.
     * 
     * @param field1 Le champ sur lequel la première requête s'effectue.
     * @param value1 Le token recherché par la première requête.
     * @param field2 Le champ sur lequel la deuxième requête s'effectue.
     * @param value2 Le token recherché par la deuxième requête.
     * @throws Exception
     */
    public void searchBoolean_Old(String field1, String value1,
                               String field2, String value2) throws Exception {
        Query q1 = new TermQuery(new Term(field1, value1));
        Query q2 = new TermQuery(new Term(field2, value2));

        Query query = new BooleanQuery.Builder()
            .add(q1, BooleanClause.Occur.MUST)   // MUST  = AND
            .add(q2, BooleanClause.Occur.MUST)   // SHOULD = OR
            .build();                            // MUST_NOT = NOT

        runQuery(query, field1 + "=" + value1 + " AND " + field2 + "=" + value2);
    }
}