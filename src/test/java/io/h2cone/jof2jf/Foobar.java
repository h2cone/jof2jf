package io.h2cone.jof2jf;

import java.util.List;

public class Foobar {
  List<Object> key1;

  String key2;

  Long key3;

  Key4 key4;

  List<Key5Elem> key5;

  Key6 key6;

  List<Double> key8;

  List<String> key9;

  List<List<List<Integer>>> key10;

  Boolean key11;

  public static class Key4 {
    Object key41;

    String key42;

    Integer key43;

    Key44 key44;

    List<Key45Elem> key45;

    public static class Key44 {
    }

    public static class Key45Elem {
      Integer key451;

      Key452 key452;

      String key453;

      public static class Key452 {
      }
    }
  }

  public static class Key5Elem {
    Long key51;
  }

  public static class Key6 {
    Integer key61;

    String key62;
  }
}
