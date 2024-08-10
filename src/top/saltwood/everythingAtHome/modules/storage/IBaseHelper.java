package top.saltwood.everythingAtHome.modules.storage;

public interface IBaseHelper<T> {
    void save() throws Exception;

    void load() throws Exception;

    T getItem();

    void setItem(T item);
}
