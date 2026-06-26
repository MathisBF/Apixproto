package org.parquet;


import org.model.ParquetRow;

import org.apache.arrow.dataset.file.FileFormat;
import org.apache.arrow.dataset.file.FileSystemDatasetFactory;
import org.apache.arrow.dataset.jni.NativeMemoryPool;
import org.apache.arrow.dataset.scanner.ScanOptions;
import org.apache.arrow.dataset.scanner.Scanner;
import org.apache.arrow.dataset.source.Dataset;
import org.apache.arrow.dataset.source.DatasetFactory;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Field;
import org.apache.arrow.vector.types.pojo.Schema;

import java.util.function.Consumer;

import java.nio.file.Path;



/**
 * La classe ParquetReader permet de lire un fichier Parquet.
 */
public class ParquetReader {



    /**
     * Retourne le schéma du fichier.
     * 
     * @param parquetPath
     * @return Le schéma parquet.
     * @throws Exception
     */
    public static Schema readSchema(Path parquetPath) throws Exception {

        try (
            BufferAllocator allocator = new RootAllocator();
            DatasetFactory factory = new FileSystemDatasetFactory(
                allocator, NativeMemoryPool.getDefault(), FileFormat.PARQUET, parquetPath.toUri().toString()
            )
        ) {
            return factory.inspect();
        }
    }



    /**
     * Lit un fichier Parquet et le stocke temporairement ligne par ligne 
     * sous forme de ParquetRow afin d'appliquer la méthode voulue sur chaque ligne.
     * 
     * @param parquetPath Le chemin vers le fichier source.
     * @param onRow La méthode à exécuter sur chaque ligne du fichier source.
     * @throws Exception
     */
    public static void readParquet(Path parquetPath, Consumer<ParquetRow> onRow) throws Exception {
        
        /*
         * Allocateur racine Arrow
         *
         * Arrow utilise de la mémoire off-heap.
         * Le RootAllocator est le point d’entrée qui trace toutes les allocations.
         * Le try-with-resources garantit qu’on détecte les fuites éventuelles.
         */
        try (
            BufferAllocator allocator = new RootAllocator();

            /*
             * FileSystemDatasetFactory est le pont Java <-> C++.
             * IMPORTANT :
             *  - Arrow attend une URI (file://...), pas un chemin brut
             *  - d’où l’appel à toUri().toString()
             */
            FileSystemDatasetFactory factory =
                new FileSystemDatasetFactory(
                    allocator,
                    NativeMemoryPool.getDefault(),
                    FileFormat.PARQUET,
                    parquetPath.toUri().toString()
                )
        ) {

            /*
             * Dataset = description logique du Parquet
             *
             * À ce stade :
             *  - aucune donnée n’est lue
             *  - aucun batch n’est chargé
             *  - on décrit simplement la source
             */
            Dataset dataset = factory.finish();

            /*
             * ScanOptions
             *
             * batchSize = nombre de lignes par RecordBatch.
             * Ce n’est ni un nombre de colonnes ni une taille mémoire.
             * Cela détermine le compromis I/O ↔ mémoire.
             */
            ScanOptions options = new ScanOptions(32_768);
            long rowCount = 0;

            /*
             * Scanner + BatchReader
             *
             * Le Scanner matérialise l’intention de lecture.
             * Les données réelles arrivent batch par batch via scanBatches().
             */
            try (
                Scanner scanner = dataset.newScan(options);
                var reader = scanner.scanBatches()
            ) {
                while (reader.loadNextBatch()) {

                    /*
                     * VectorSchemaRoot représente un RecordBatch Arrow :
                     * - un ensemble de colonnes
                     * - toutes de même longueur (getRowCount)
                     */
                    VectorSchemaRoot root = reader.getVectorSchemaRoot();

                    /*
                     * Schéma
                     *
                     * En Arrow Dataset, le schéma réel n’est connu
                     * qu’une fois un batch chargé.
                     * On l’affiche donc sur le premier batch uniquement.
                     */
                    if (rowCount == 0) {
                        Schema schema = root.getSchema();
                        System.out.println("\n=== Columns ===");
                        for (Field field : schema.getFields()) {
                            System.out.printf(
                                "%s : %s%n",
                                field.getName(),
                                field.getType()
                            );
                        }
                        System.out.println();
                    }

                    /**
                     * Pour chaque ligne contenue dans le batch,
                     * la stocke sous forme de ParquetRow
                     * et exécute la méthode onRow sur chaque row.
                     */
                    for (int rowIndex = 0; rowIndex < root.getRowCount(); rowIndex++) {

                        ParquetRow row = new ParquetRow();

                        for (int colIndex = 0; colIndex < root.getFieldVectors().size(); colIndex++) {
                            String colName = root.getVector(colIndex).getName();
                            Object value = root.getVector(colIndex).getObject(rowIndex);
                            row.put(colName, value);
                        }

                        onRow.accept(row);
                    }

                    rowCount += root.getRowCount();
                }
            }
        }
    }
}
