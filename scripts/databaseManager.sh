init() {
    echo $"[INFO] databaseManager - $(sudo service redis-server start)"
    echo $"[INFO] databaseManager - Loaded database with $(redis-cli dbsize) entries."
}

close() {
    return=$(redis-cli save)
    echo $"[INFO] databaseManager - Saving database with $(redis-cli dbsize) entries."
    echo $"[INFO] databaseManager - Closing. $(redis-cli shutdown)"
}

mode="$1"

if [[ "$mode" == "init" ]]; then
    init
elif [[ "$mode" == "close" ]]; then
    close
fi