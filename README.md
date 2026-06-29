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
- Lecture du fichier Parquet, la conversion des types de Arrow à Lucenne à traves la classe ParquetSchema, nottament choix entre StringField et TextField.
- Relier ParquetSchema à LuceneIndexer.
- Système de requête, trop pauvre et ne correspondant pas encore à tous les besoins.
- Mettre à jour Arrow.
