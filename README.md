# CS 157C Final Project

## Datasets used

* Nodes & edges (SNAP): https://snap.stanford.edu/data/ego-Twitter.html
* Synthetic profile data (Kaggle): https://www.kaggle.com/datasets/amaleshvemula7/name-and-country-of-origin-dataset

## Quick Start

1. Prerequisites:
    * Have Python installed
    * Have IntellIJ IDEA installed

1. Start Neo4j Docker container:
    ```
    cd docker && docker compose up -d
    ```
2. Generate Cypher queries from the datasets:
    ```
    cd dataset && python3 gen_cypher.py
    ```
3. Populate the database (this will take a while):
    ```
    chmod +x ./populate_database.sh
    ./populate_database.sh
    ```