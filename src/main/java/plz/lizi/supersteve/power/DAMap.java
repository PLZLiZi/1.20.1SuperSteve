package plz.lizi.supersteve.power;

import java.util.*;

@SuppressWarnings("all")
public class DAMap<K, V> implements Iterable<DAMap.Entry<K, V>> {
    private static final Object TOMBSTONE = new Object();
    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private Object[] table;
    private int size;
    private int threshold;

    public DAMap() {
        this.table = new Object[DEFAULT_CAPACITY * 2];
        this.threshold = (int) (DEFAULT_CAPACITY * DEFAULT_LOAD_FACTOR);
    }

    public synchronized int size() {
        return size;
    }

    public synchronized boolean isEmpty() {
        return size == 0;
    }

    public synchronized V put(K key, V value) {
        if (key == null)
            throw new NullPointerException("key");
        if (size >= threshold)
            resize();
        int index = findIndex(key);
        V oldValue = (V) table[index + 1];
        table[index] = key;
        table[index + 1] = value;
        if (oldValue == null || oldValue == TOMBSTONE) {
            size++;
        }
        return oldValue == TOMBSTONE ? null : oldValue;
    }

    public synchronized V get(Object key) {
        if (key == null)
            return null;
        int index = findIndex(key);
        V value = (V) table[index + 1];
        return value == TOMBSTONE ? null : value;
    }

    public synchronized V remove(Object key) {
        if (key == null)
            return null;
        int index = findIndex(key);
        V value = (V) table[index + 1];
        if (value != null && value != TOMBSTONE) {
            table[index] = TOMBSTONE;
            table[index + 1] = TOMBSTONE;
            size--;
            return value;
        }
        return null;
    }

    public synchronized void clear() {
        Arrays.fill(table, null);
        size = 0;
    }

    public synchronized boolean containsKey(Object key) {
        return key != null && get(key) != null;
    }

    public synchronized boolean containsValue(Object value) {
        for (int i = 1; i < table.length; i += 2) {
            if (table[i] != null && table[i] != TOMBSTONE && table[i].equals(value)) {
                return true;
            }
        }
        return false;
    }

    public synchronized V putIfAbsent(K key, V value) {
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }
        return v;
    }

    public synchronized V getOrDefault(Object key, V defaultValue) {
        V v = get(key);
        return v != null ? v : defaultValue;
    }

    private synchronized int hash(Object key) {
        int h = key.hashCode();
        return h ^ (h >>> 16);
    }

    private synchronized int findIndex(Object key) {
        int hash = hash(key);
        int index = (hash & 0x7FFFFFFF) % (table.length / 2) * 2;
        while (table[index] != null) {
            if (table[index] != TOMBSTONE && table[index].equals(key)) {
                return index;
            }
            index = (index + 2) % table.length;
        }
        return index;
    }

    private synchronized void resize() {
        Object[] oldTable = table;
        int newCapacity = oldTable.length * 2;
        Object[] newTable = new Object[newCapacity];
        for (int i = 0; i < oldTable.length; i += 2) {
            if (oldTable[i] != null && oldTable[i] != TOMBSTONE) {
                K key = (K) oldTable[i];
                V value = (V) oldTable[i + 1];
                int hash = hash(key);
                int newIndex = (hash & 0x7FFFFFFF) % (newCapacity / 2) * 2;
                while (newTable[newIndex] != null) {
                    newIndex = (newIndex + 2) % newCapacity;
                }
                newTable[newIndex] = key;
                newTable[newIndex + 1] = value;
            }
        }
        this.table = newTable;
        this.threshold = (int) ((newCapacity / 2) * DEFAULT_LOAD_FACTOR);
    }

    @Override
    public synchronized Iterator<Entry<K, V>> iterator() {
        return new Iterator<Entry<K, V>>() {
            private int nextIndex = 0;

            @Override
            public boolean hasNext() {
                while (nextIndex < table.length) {
                    if (table[nextIndex] != null && table[nextIndex] != TOMBSTONE)
                        return true;
                    nextIndex += 2;
                }
                return false;
            }

            @Override
            public Entry<K, V> next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                Entry<K, V> entry = new Entry<>((K) table[nextIndex], (V) table[nextIndex + 1]);
                nextIndex += 2;
                return entry;
            }
        };
    }

    public synchronized Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private final Iterator<Entry<K, V>> it = DAMap.this.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public K next() {
                        return it.next().getKey();
                    }
                };
            }

            @Override
            public int size() {
                return DAMap.this.size();
            }
        };
    }

    public synchronized Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private final Iterator<Entry<K, V>> it = DAMap.this.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public V next() {
                        return it.next().getValue();
                    }
                };
            }

            @Override
            public int size() {
                return DAMap.this.size();
            }
        };
    }

    public synchronized Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return DAMap.this.iterator();
            }

            @Override
            public int size() {
                return DAMap.this.size();
            }
        };
    }

    public static class Entry<K, V> {
        private final K key;
        private final V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Entry))
                return false;
            Entry<?, ?> that = (Entry<?, ?>) o;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        Iterator<Entry<K, V>> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }
}
