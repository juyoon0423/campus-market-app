import { describe, expect, it } from "vitest";
import { decodeUserIdFromToken, findNumericUserId } from "@/src/hooks/useCurrentUserId";

function makeToken(payload: Record<string, unknown>) {
  const base64Url = (value: string) =>
    Buffer.from(value).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");

  const header = base64Url(JSON.stringify({ alg: "none" }));
  const body = base64Url(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

describe("decodeUserIdFromToken", () => {
  it("userId 클레임이 있으면 숫자로 반환한다", () => {
    const token = makeToken({ userId: 42 });
    expect(decodeUserIdFromToken(token)).toBe(42);
  });

  it("userId가 없으면 sub 클레임을 사용한다", () => {
    const token = makeToken({ sub: "7" });
    expect(decodeUserIdFromToken(token)).toBe(7);
  });

  it("토큰이 null이면 null을 반환한다", () => {
    expect(decodeUserIdFromToken(null)).toBeNull();
  });

  it("payload가 JWT 형식이 아니면 null을 반환한다", () => {
    expect(decodeUserIdFromToken("not-a-jwt")).toBeNull();
  });

  it("payload를 JSON으로 파싱할 수 없으면 null을 반환한다", () => {
    const malformed = `${Buffer.from("{}").toString("base64")}.not-base64-json.sig`;
    expect(decodeUserIdFromToken(malformed)).toBeNull();
  });
});

describe("findNumericUserId", () => {
  it("숫자 값을 그대로 반환한다", () => {
    expect(findNumericUserId(5)).toBe(5);
  });

  it("숫자로 변환 가능한 문자열을 반환한다", () => {
    expect(findNumericUserId("10")).toBe(10);
  });

  it("우선순위 키(userId > id > user_id > memberId > sub) 순서로 탐색한다", () => {
    expect(findNumericUserId({ id: 2, userId: 1 })).toBe(1);
    expect(findNumericUserId({ sub: "4", memberId: 3 })).toBe(3);
  });

  it("배열이면 첫 번째로 발견되는 숫자를 반환한다", () => {
    expect(findNumericUserId(["not-a-number", 9])).toBe(9);
  });

  it("숫자를 찾을 수 없으면 null을 반환한다", () => {
    expect(findNumericUserId({ name: "no id here" })).toBeNull();
  });
});
