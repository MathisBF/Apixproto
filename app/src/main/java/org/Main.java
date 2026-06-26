package org;


import java.nio.file.Path;
import java.util.Scanner;

import org.indexer.LuceneIndexer;
import org.searcher.LuceneSearcher;

import org.parquet.ParquetReader;



/**
 * La classe Main de l'application.
 */
public class Main {

    /**
     * La méthode main de l'app.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.err.println("Usage: java Main <path-to-parquet-file> <path-to-index-dir>");
            System.exit(1);
        }

        Path parquetPath = Path.of(args[0]);
        Path indexPath = Path.of(args[1]);


        if (!LuceneSearcher.indexExists(indexPath)) {
            System.out.println("\nNo index found.\nBuilding the index ...\n");
            buildIndex(parquetPath, indexPath);
        }

        try (LuceneSearcher searcher = new LuceneSearcher(indexPath)) {
            runConsole(parquetPath, indexPath, searcher);
        }
    }



    /**
     * Construit un index Lucene à partir d'un fichier Parquet.
     * 
     * @param parquetPath Le chemin vers le fichier source.
     * @param indexPath Le chemin vers le fichier destinataire, afin de stocker l'index.
     * @throws Exception
     */
    private static void buildIndex(Path parquetPath, Path indexPath) throws Exception {

        try (LuceneIndexer indexer = new LuceneIndexer(indexPath)) {
            ParquetReader.readParquet(parquetPath, row -> {
                try {
                    indexer.writeIndex(row);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            indexer.commit();
        }
    }



    /**
     * Exécute et affiche les résultats d'une série de requêtes préécrites.
     * 
     * @param searcher Le searcher, voir LuceneSearcher.
     * @throws Exception
     */
    private static void search(LuceneSearcher searcher) throws Exception {
        System.out.println("\n=== Recherches ===\n");

        // Valeur exacte
        searcher.searchExact_Old("ville", "Paris");
        searcher.searchExact_Old("metier", "ingénieur");

        // Texte libre sur description
        searcher.searchText_Old("description", "logiciels");

        // Plage numérique
        searcher.searchLongRange_Old("age", 25, 35);
        searcher.searchDoubleRange_Old("salaire", 50000, 70000);

        // Combinaison AND
        searcher.searchBoolean_Old("ville", "Paris", "metier", "ingénieur");
    }



    ///**
    // * Effectue une requête textuel sur un champ.
    // * Utilise LuceneSearcher.searchText.
    // * 
    // * @param stringField Le nom du champ.
    // * @param stringQuery La requête.
    // * @param searcher Le searcher, voir LuceneSearcher.
    // * @throws Exception
    // */
    //public static void homeSearchOnField_Old(String stringField, String stringQuery, LuceneSearcher searcher) throws Exception {
    //    searcher.searchText(stringField, stringQuery);
    //}



    /**
     * Effectue une requête textuel sur un champ.
     * Utilise LuceneSearcher.searchTextNew et LuceneSearcher.printResults.
     * 
     * @param stringField Le nom du champ.
     * @param stringQuery La requête.
     * @param searcher Le searcher, voir LuceneSearcher.
     * @throws Exception
     */
    public static void homeSearch(String stringField, String stringQuery, LuceneSearcher searcher) throws Exception {
        if (stringField.equals("ALL")) {
            searcher.printResults(
                searcher.largeSearch(stringQuery, 10)
            );
        } else {
            searcher.printResults(
                searcher.searchText(stringField, stringQuery, 10)
            );
        }
    }



    /**
     * Gère les interactions humain / machine et lance l'exécutions des fonctions désirées de l'app.
     * 
     * @param parquetPath Le chemin vers le fichier source.
     * @param indexPath Le chemin vers l'index.
     * @param searcher Le searcher, voir LuceneSearcher.
     * @throws Exception
     */
    private static void runConsole(Path parquetPath, Path indexPath, LuceneSearcher searcher) throws Exception {

        int choix = 0;
        String[] query = new String[2];

        try (Scanner scanner = new Scanner(System.in);) {

            while (choix != 4) {
                System.out.println("\n\n=== Menu ===\n");
                System.out.println("1 - Rebuild the Index");
                //System.out.println("2 - Do a research _ Old");
                System.out.println("2 - Do a research");
                System.out.println("3 - Run prewritted researches");
                System.out.println("4 - Leave the app");
                System.out.print("\nyour choice : ");

                choix = scanner.nextInt();
                scanner.nextLine();
                switch (choix) {

                    case 1:
                        try {
                            buildIndex(parquetPath, indexPath);
                            searcher.refresh();
                            System.out.println("\nIndex rebuilt.\n");
                        } catch (Exception e) {
                            System.err.println("Error during the indexation : " + e.getMessage());
                        }
                        break;

                    //case 2:
                    //    System.out.print("\n--- Your query ---\nfield : ");
                    //    query[0] = scanner.nextLine();
                    //    System.out.print("text : ");
                    //    query[1] = scanner.nextLine();
                    //    homeSearchOnField_Old(query[0], query[1], searcher);
                    //    break;
                    
                    case 2:
                        System.out.print("\n--- Your query ---\nfield : ");
                        query[0] = scanner.nextLine();
                        System.out.print("text : ");
                        query[1] = scanner.nextLine();
                        homeSearch(query[0], query[1], searcher);
                        break;

                    case 3:
                        try {
                            search(searcher);
                        } catch (Exception e) {
                            System.err.println("Error during the research : " + e.getMessage());
                        }
                        break;

                    case 4:
                        System.out.println("\n\n=== Leaving the app ===");
                        break;
                
                    default:
                        System.err.println("\nError : Non valuable choice\nWrite one of the proposed number");
                        break;
                }
            }
        }
    }
}
