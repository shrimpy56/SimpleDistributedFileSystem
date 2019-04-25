service FileServer
{
    i32 getVersionOf(1: string filename);
    //forward
    string read(1: string filename);
    void write(1: string filename, 2: string contents);
    //for coordinator
    string doRead(1: string filename);
    void doWrite(1: string filename, 2: string contents, 3: i32 version);
    string getFileList();

    // for coordinator
    bool join(1: string IP, 2: i32 port);
}
