package org.optum.erp.search.model;
import java.util.ArrayList;
import java.util.List;
public class QueryResults
{
    private List<QueryResult> queryResult;

    public void setQueryResult(List<QueryResult> queryResult){
        this.queryResult = queryResult;
    }
    public List<QueryResult> getQueryResult(){
        return this.queryResult;
    }
}