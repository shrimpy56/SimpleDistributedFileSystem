//struct Info
//{
//    1: string ip,
//    2: i32 port,
//}

service FileServer
{
    i32 getVersionOf(1: string filename);
    //forward
    string read(1: string filename);
    void write(1: string filename, 2: string contents);
    //for coordinator
    string doRead(1: string filename);
    void doWrite(1: string filename, 2: string contents);
    void clearFiles();
    string getFileList();
}
