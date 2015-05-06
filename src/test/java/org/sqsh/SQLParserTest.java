/*
 * IBM Confidential
 * OCO Source Materials
 * (C) Copyright IBM Corp. 2010, 2014
 * The source code for this program is not published or otherwise divested of
 * its trade secrets, irrespective of what has been deposited with the U.S. 
 * Copyright Office.
 */
package org.sqsh;

import org.junit.Test;
import org.junit.Assert;
import org.sqsh.SQLIdentifierNormlizer.Direction;
import org.sqsh.input.completion.CompletionCandidate;
import org.sqsh.input.completion.SQLParseState;
import org.sqsh.parser.DatabaseObject;
import org.sqsh.parser.SQLParser;
import org.sqsh.parser.SimpleSQLTokenizer;

public class SQLParserTest {
    
    @Test
    public void testTokenizer() {
        
        SimpleSQLTokenizer t = new SimpleSQLTokenizer("select * from \"MySchema\".\"MyTable\"");
        Assert.assertEquals("select", t.next());
        Assert.assertEquals("*", t.next());
        Assert.assertEquals("from", t.next());
        Assert.assertEquals("\"MySchema\"", t.next());
        Assert.assertEquals(".", t.next());
        Assert.assertEquals("\"MyTable\"", t.next());
    }
    
    @Test
    public void testSQLParser() {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.NONE, Direction.NONE), info);
        
        parser.parse("select count(*) from");
        
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertTrue(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(0, refs.length);
        
        info.reset();
        parser.parse("select count(*) from t1");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("t1", refs[0].getName());
        
        info.reset();
        parser.parse("select count(*) from mycatalog.myschema.mytable a where");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("mycatalog", refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertEquals("a", refs[0].getAlias());
        
        // Make sure keywords aren't seen an alias
        info.reset();
        parser.parse("select count(*) from myschema.mytable where");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertNull(refs[0].getAlias());
        
        info.reset();
        parser.parse("select count(*) from myschema.mytable having");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertNull(refs[0].getAlias());
        
        // "order" should be an alias
        info.reset();
        parser.parse("select count(*) from myschema.mytable order where x = 10");
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertEquals("order", refs[0].getAlias());
        
        // "order" should not be an alias
        info.reset();
        parser.parse("select count(*) from myschema.mytable order by c1");
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertNull(refs[0].getAlias());
        
        // "group" should be an alias
        info.reset();
        parser.parse("select count(*) from myschema.mytable group where x = 10");
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertEquals("group", refs[0].getAlias());
        
        // "group" should not be an alias
        info.reset();
        parser.parse("select c1, count(*) from myschema.mytable group by c1");
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertNull(refs[0].getAlias());
        
        // "having" should not be an alias
        info.reset();
        parser.parse("select count(*) from myschema.mytable having count(*) > 2");
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertNull(refs[0].getCatalog());
        Assert.assertEquals("myschema", refs[0].getSchema());
        Assert.assertEquals("mytable", refs[0].getName());
        Assert.assertNull(refs[0].getAlias());
        
        info.reset();
        parser.parse("select count(*) from (select c1, c2");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(0, refs.length);
        
        info.reset();
        parser.parse("select count(*) from (select c1, c2) t1");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("t1", refs[0].getName());
        String columns[] = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(2, columns.length);
        Assert.assertEquals("c1", columns[0]);
        Assert.assertEquals("c2", columns[1]);
        
        info.reset();
        parser.parse("select count(*) from (select c1, c2) as t1 (a, b)");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("t1", refs[0].getName());
        columns = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(2, columns.length);
        Assert.assertEquals("a", columns[0]);
        Assert.assertEquals("b", columns[1]);
        
        info.reset();
        parser.parse("select count(*) from (select c1, c2) as t1 (a, b) left outer join t2 on t1.a = t2.b right join t3");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("t1", refs[0].getName());
        columns = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(2, columns.length);
        Assert.assertEquals("a", columns[0]);
        Assert.assertEquals("b", columns[1]);
        Assert.assertEquals("t2", refs[1].getName());
        Assert.assertEquals("t3", refs[2].getName());
        
        info.reset();
        parser.parse("select count(*) from (select c1, c2) as t1 (a, b) left outer join t2 on t1.a = t2.b right join t3 on");
        Assert.assertEquals("SELECT", info.getStatement());
        Assert.assertFalse(info.isEditingTableReference());
    }
    
    @Test
    public void testAnsiJoin () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("select count(*) from t1 left join t2");
        
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(2, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        
        info.reset();
        parser.parse("select count(*) from t1 left join t2 right outer join t3");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        
        info.reset();
        parser.parse("select count(*) from ((t1 left join t2 on t1.a = t2.a) right outer join t3 on t1.b t3.c)");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        
        info.reset();
        parser.parse("select count(*) from ((t1 left join t2 on t1.a = t2.a) right outer join t3 on t1.b t3.c) where");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        
        info.reset();
        parser.parse("select count(*) from ((t1 left join t2 on t1.a = t2.a) right outer join t3 on t1.b t3.c) inner join t4");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(4, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        Assert.assertEquals("T4", refs[3].getName());
        
        info.reset();
        parser.parse("select count(*) from (((t1 left join t2 on t1.a = t2.a) right outer join t3 on t1.b t3.c) inner join t4) cross join t5");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(5, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        Assert.assertEquals("T4", refs[3].getName());
        Assert.assertEquals("T5", refs[4].getName());
        
        info.reset();
        parser.parse("select count(*) from t1 left outer join (select a, b from a1, a2 where a1.a = a2.b) x1");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(2, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("X1", refs[1].getName());
        
        info.reset();
        parser.parse("select count(*) from t1 left outer join (select a, b from a1, a2");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("A1", refs[1].getName());
        Assert.assertEquals("A2", refs[2].getName());
        
        info.reset();
        parser.parse("select count(*) from table(mything.foo(10, 2)) t1, t2");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(2, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        
        // syntax error
        info.reset();
        parser.parse("select count(*) from table(mything.foo(10, 2)), t2");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T2", refs[0].getName());
        
        info.reset();
        parser.parse("select count(*) from table(mything.foo(10, 2)) as t1 (a, b, c), t2");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(2, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        String []columns = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(3, columns.length);
        Assert.assertEquals("a", columns[0]);
        Assert.assertEquals("b", columns[1]);
        Assert.assertEquals("c", columns[2]);
        Assert.assertEquals("T2", refs[1].getName());
    }
    
    @Test
    public void testMultipleStatements () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("select count(*) from t1, t2 where t1.a = t2.a; select a, b from c");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("C", refs[0].getName());
        
        parser.parse("select count(*) from t1, t2 where t1.a = t2.a select a, b from c");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("C", refs[0].getName());
    }
    
    @Test
    public void testTricky () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse(
            "select count(*) from "
                + "(select (select 10) * avg(c) d, a / 2 f from t2) a left join b on a.c1 = b.c2 where x > 10");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(2, refs.length);
        Assert.assertEquals("A", refs[0].getName());
        String []columns = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(2, columns.length);
        Assert.assertEquals("D", columns[0]);
        Assert.assertEquals("F", columns[1]);
        Assert.assertEquals("B", refs[1].getName());
        
        // Survive syntax errors
        parser.parse("select * from (select count(*) from), t1");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
    }
    
    @Test
    public void testInsert () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("insert t1 values (10, 20, 30)");
        Assert.assertEquals("INSERT", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        
        parser.parse("insert into \"My Catalog\".s1.t1 values (10, 20, 30)");
        Assert.assertEquals("INSERT", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("S1", refs[0].getSchema());
        Assert.assertEquals("My Catalog", refs[0].getCatalog());
        
        parser.parse("insert t1 select count(*) from t2");
        Assert.assertEquals("SELECT", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T2", refs[0].getName());
    }
    
    @Test
    public void testUpdate () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("update \"Catalog\".\"Schema\".\"Table\" set c2 = 10 where c4 = 30");
        Assert.assertEquals("UPDATE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("Catalog", refs[0].getCatalog());
        Assert.assertEquals("Schema", refs[0].getSchema());
        Assert.assertEquals("Table", refs[0].getName());
        
        parser.parse("update t1 set c1 = 5 from t1, t2");
        Assert.assertEquals("UPDATE", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T1", refs[1].getName());
        Assert.assertEquals("T2", refs[2].getName());
        
        parser.parse("update t1 set c1 = 5 from t1, t2 where t1.a = t2.b");
        Assert.assertEquals("UPDATE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T1", refs[1].getName());
        Assert.assertEquals("T2", refs[2].getName());
        
        parser.parse("update (select a, b, c from t2) as t4 set c1 = 5 where t1.a = t2.b");
        Assert.assertEquals("UPDATE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T4", refs[0].getName());
        String []columns = refs[0].getColumnList();
        Assert.assertNotNull(columns);
        Assert.assertEquals(3, columns.length);
        Assert.assertEquals("A", columns[0]);
        Assert.assertEquals("B", columns[1]);
        Assert.assertEquals("C", columns[2]);
        
        parser.parse("update top 5 c1.s1.t1 set c1 = 5 from t1, t2 where t1.a = t2.b");
        Assert.assertEquals("UPDATE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("C1", refs[0].getCatalog());
        Assert.assertEquals("S1", refs[0].getSchema());
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T1", refs[1].getName());
        Assert.assertEquals("T2", refs[2].getName());
    }
    
    @Test
    public void testDelete () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("delete from t1");
        Assert.assertEquals("DELETE", info.getStatement());
        // Assert.assertTrue(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        
        parser.parse("delete from t1 where");
        Assert.assertEquals("DELETE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        
        parser.parse("delete from t1, t2, t3 where");
        Assert.assertEquals("DELETE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(3, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        Assert.assertEquals("T2", refs[1].getName());
        Assert.assertEquals("T3", refs[2].getName());
        
        parser.parse("delete top 4 from t1 where");
        Assert.assertEquals("DELETE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
        
        parser.parse("delete top 4 t1 where");
        Assert.assertEquals("DELETE", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("T1", refs[0].getName());
    }
    
    @Test
    public void testCall () {
        
        SQLParseState info = new SQLParseState();
        SQLParser parser = new SQLParser(new SQLIdentifierNormlizer(Direction.UPPER_CASE, Direction.NONE), info);
        
        parser.parse("call c1.s1.p1 (");
        Assert.assertEquals("CALL", info.getStatement());
        // Assert.assertFalse(info.isEditingTableReference());
        DatabaseObject []refs = info.getObjectReferences();
        Assert.assertEquals(1, refs.length);
        Assert.assertEquals("C1", refs[0].getCatalog());
        Assert.assertEquals("S1", refs[0].getSchema());
        Assert.assertEquals("P1", refs[0].getName());
    }
}
