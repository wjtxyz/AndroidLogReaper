## Android Private Log Reaper
##### collect the private log from multi-module & multi-process

Demonstrate three methods to collect the private log, and diff them

[Socket](https://developer.android.com/reference/java/net/Socket "Socket") & [Pipe (Provider)](https://developer.android.com/reference/android/content/ContentProvider.html#openFile&#40;android.net.Uri,%2520java.lang.String&#41; "Pipe (Provider)") & [EventLog](https://developer.android.com/reference/android/util/EventLog "EventLog") 

|   |addition thread(client side)   |independency client & server   |server start by client   | FileDescriptor count   |BufferSize   |
| ------------ | ------------ | ------------ | ------------ | ------------ | ------------ |
| SocketLog  |  &check;    |&check;   |&times;  |N/N client   |128K (user changable)  |
| ProviderLog  | &times;    | &check;  | &check;    | 1/N client  |4K   |
| EventLog  | &times;     | &times;    |  &times;    | 0  | 256K (user changable)  |


**additional thread (Client side)**: Android forbid the network call on MainThread, so require additional thread to send socket message

**independency Client & Server**:  when Client start log message, whether Server must start at same time

**Server start by client**: Client can start Server if Server process crash or exit unexpectedly

**FileDescriptor count**: if there are too many clients connect Server at same time, Server process may fail because of FD size limit per process(1024)


