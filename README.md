# ApixProto

ApixProto est un prototype d'Apix, un module d'indexation et de recherche via Lucene avec en entrée un fichier Parquet.

---

## Prérequis et Caractéristiques
### Java 26.0.1
### Gradle 9.5.1
### Lucene 10.4.0
### Arrow 15.0.2
La version 17.0.0 existe.

---

## Fonctionnalités présentes
- Lecture d'un fichier au format Parquet et création d'un index Lucene à partir du fichier source.
- Exécution de requête sur l'index via Lucene.

---

## Points à développer
- Conversion des types de Arrow à Lucenne à traves la classe IndexSchema, nottament choix entre StringField et TextField.
- Système de requête, trop pauvre et ne correspondant pas aux besoins.
- Mettre à jour Arrow.

---

## Architecture du projet

```text
app
├── data
│   └── index
└── src
    └── main
        ├── java
        │   └── org
        │       ├── indexer
        │       ├── model
        │       ├── parquet
        │       └── searcher
        └── resources
```

### parquet

Contient `ParquetReader.java`, la classe permettant de lire le fichier Parquet (fichier se trouvant dans `resources`), composée de deux méthodes:
- `readSchema(Path parquetPath)` retournant le schema Arrow des données du fichier.
- `readParquet(Path parquetPath,  Consumer<ParquetRow> onRow)` lisant le fichier batch par batch, pour chaque ligne, construis un objet `ParquetRow` contenant les données de la ligne Parquet, et appelle la méthode onRow sur cet objet.

### indexer

### model

### searcher

---

## Suivi du projet

### 15/06 -> 30/06

### 30/06

L'application permet de
- lire un fichier Parquet
- créer un index Lucene à partir de ce fichier source et stocke l'index dans des fichiers
- effectuer des recherches / requêtes sur cet index

## Structure du projet

```text
app
├── data
│   └── index
└── src
    └── main
        ├── java
        │   └── org
        │       ├── indexer
        │       ├── model
        │       ├── parquet
        │       └── searcher
        └── resources
```