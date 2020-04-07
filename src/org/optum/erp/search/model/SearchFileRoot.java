package org.optum.erp.search.model;
public class SearchFileRoot
{
    private QueryResults queryResults;

    public void setQueryResults(QueryResults queryResults){
        this.queryResults = queryResults;
    }
    public QueryResults getQueryResults(){
        return this.queryResults;
    }
}