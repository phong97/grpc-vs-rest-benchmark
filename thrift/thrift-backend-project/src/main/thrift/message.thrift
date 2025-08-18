namespace java com.thrift.thriftbackend;

struct Message {
  1: required string phone,
  2: required string templateId,
  3: optional map<string, string> templateData,
  4: optional string trackingId,
}

struct Quota {
    1: optional string dailyQuota,
    2: optional string remainingQuota,
}

struct MessageResponse {
  1: required i32 error,
  2: required string message,
  3: optional string msgId,
  4: optional i64 sendTime,
  5: optional string sendingMode,
  6: optional Quota quota,
}
