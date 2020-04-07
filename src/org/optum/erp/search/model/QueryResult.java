package org.optum.erp.search.model;
public class QueryResult
{
    private Document document;

    private int score;

    public void setDocument(Document document){
        this.document = document;
    }
    public Document getDocument(){
        return this.document;
    }
    public void setScore(int score){
        this.score = score;
    }
    public int getScore(){
        return this.score;
    }
}