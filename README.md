# ApixProto

ApixProto est un prototype d'Apix, un module d'indexation et de recherche via Lucene avec en entrée un fichier Parquet.

Le module doit pouvoir, lire un fichier au format Parquet, stocker ses données sous forme d'un index Lucene, et effectuer des requêtes sur l'index ainsi obtenu.

---

## Prérequis et Caractéristiques
### Java 26.0.1
### Gradle 9.5.1
### Lucene 10.4.0
### Arrow 15.0.2
La version 17.0.0 existe.

---

## Utilisation

./gradlew run --console=plain --args="src/main/resources/test.parquet data/index" --no-configuration-cache

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

Le répertoire `index` sert comme son nom l'indique à stocker l'index généré par l'application, et `resources` a pour but de contenir le fichier source.

### parquet

Contient `ParquetReader.java`, la classe permettant de lire le fichier Parquet (fichier se trouvant dans `resources`), composée de deux méthodes:
- `readSchema(Path parquetPath)` retournant le schema Arrow des données du fichier.
- `readParquet(Path parquetPath,  Consumer<ParquetRow> onRow)` lisant le fichier batch par batch, pour chaque ligne du fichier Parquet, construis un objet `ParquetRow` contenant les données de la ligne Parquet, et appelle la méthode onRow sur cet objet.

### model

Structure les données via deux classes:
- `ParquetRow` représente une ligne d'un fichier Parquet sous la forme d'un dictionnaire, chaque paire clé / valeur est associée à un field Arrow contenant une seule valeur (celle de la ligne du fichier correspondante). La clé du dictionnaire est le nom du field, et sa valeur est la valeur du field de cette ligne, créer comme instance d'Object java.
- `IndexSchema` contient les métatdonnées de l'index, et fait le lien entre les données Arrow et Lucene. Permet nottament de déterminer comment convertir les données Arrow en fields Lucene afin des les stocker dans l'index. Les métadonnées sont stockées dans l'index avec l'indexer afin d'être accessibles depuis le searcher et facilter ainsi la formulation des requêtes.

### indexer

Contient la classe `LuceneIndexer`, qui permet l'écriture de l'index. L'écriture se fait à partir d'un ParquetRow qui est convertit en un Document Lucene via IndexSchema, pour se faire sa méthode `writeIndex(ParquetRow row)` est appelée en entrée de `readParquet(Path parquetPath,  Consumer<ParquetRow> onRow)` à la place de `onRow`.

### searcher

Contient la classe `LuceneSearcher` qui permet la formulation et l'exécution des requêtes / recherches sur l'index.

---

## Correspondances Arrow Lucene

| Type Arrow | Type Lucene | Recherches possibles |
|------------|-------------|----------------------|
| `Utf8`, `LargeUtf8` | `StringField` / `TextField` | valeur exacte / texte libre |
| Bool | `StringField` ("true" / "false") | valeur exacte |
| `Int(8)`, `Int(16)`, `Int(32)` | `IntPoint` + `StoredField` | valeur exacte / intervalle |
| `Int(64)` | `LongPoint` + `StoredField` | valeur exacte / intervalle |
| `FloatingPoint(HALF)`, `FloatingPoint(SINGLE)` | `FloatPoint` + `StoredField` | valeur exacte / intervalle |
| `FloatingPoint(DOUBLE)` | `DoublePoint` + `StoredField` | valeur exacte / intervalle |
| `TimeStamp`, `Date` | `LongPoint` + `StoredField` | intervalle |

---

## Suivi du projet

### CheckPoint - 30/06

#### Lancement du projet

Prise en main de Lucene et Arrow.

Début du développement à partir du fichier `ParquetReader.java` qui était préexistant, création de `LuceneIndexer` et de `ParquetRow` pour écrire les données du fichier source Parquet (utilisation alors d'un fichier de test de 10 lignes) dans un index Lucene. Puis création de `LuceneSearcher` pour lire l'index et faire des requêtes dessus.

L'application permet alors de
- lire un fichier Parquet
- créer un index Lucene à partir de ce fichier source et stocke l'index dans des fichiers
- effectuer des recherches / requêtes sur cet index

Amélioration des différentes classes, méthodes `toString()` et `equals()` avec `@override`, faire en sorte que `LuceneIndexer` et `LuceneSearcher` implémentent `AutoCloseable` ...

Création de `IndexSchema` afin de stocker les métadonnées de l'index.

Ajout d'une couche de conversion entre `ParquetReader` et `LuceneIndexer` via `IndexSchema` afin de mieux séparer les rôles de chaque classe.

Stockage de `IndexSchema` dans l'index, et non plus via un objet java. Permet l'autonomie de `LuceneSearcher`.

Mise à jour des types de fields numériques Lucene, exemple :
    LongField -> LongPoint + StoredField (disponibles depuis Lucene 8/9)

Finalisation des classes socles (`ParquetReader` et `LuceneIndexer`, qui ne dépendent pas de la façon précise de convertir les données car je n'ai pas encore accès au bon fichier source), sauf problème survenant ultérieurement.

#### Architecture du projet à cette étape :

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

#### To do

- Revoir la conversion des types de Arrow à Lucenne pour quelle soit adapté au projet, nottament choix entre StringField et TextField. Ajouter NumericDocValues (permet tri par exemple) ?
- Mettre au propre le système de requête et l'enrichir.
- Mettre à jour Arrow.
- Création de tests avec JUnit.
