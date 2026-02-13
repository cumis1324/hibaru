
import base64
import json

def decode_new_obfuscation(text):
    # Prefix: Y29kZWlzcHJvdGVjdGVk (codeisprotected)
    # It seems logic is:
    # 1. Remove prefix 'codeisprotected' (16 chars?) -> No, base64 is Y29kZWlzcHJvdGVjdGVk (16 chars decoded = codeisprotected)
    # The raw text STARTS with Y29kZWlzcHJvdGVjdGVk
    
    # Analysis:
    # Sample: Y29kZWlzcHJvdGVjdGVk==QfioVM1QjL4MjOwMjOzADV2ETL0ATL1IDMyIiOiUWbpRFZllmZpR2btJCLioVM1QjL4MjOwMjOzADV2ETL0ATL1IDMyIiOiUWbpRFZlRXYlJ3YiwiIkVmchh2crIXZkx2bm5ycwBXYtUGbn92bn5CZuZ3Lu9Wa0F2YpxGcwF2LlBXe09iNx8SbvNmL05WZ052bjJXZzVXZsd2bvdmL5RnchBHZylGa01SZ2lmck9yL6MHc0RHaiojIr5WaM52bjlmIsIiclRGbvZmLzBHch1SZsd2bvdmLk5mdv42bpRXYjlGbwBXYiojIlBXeUVWbp1mIsIya0IiOiUWbh5mIsIiTWZGeUp1bhZ2YuN0bmFnVRd3YXdWaygzSlN0MHFFUUFjI6ICZpJyeYmFzZTY0aXNleGNsdWRlZA==
    
    # Try Java Logic EXACTLY on the whole string:
    # 1. Reverse
    rev = text[::-1]
    
    # 2. Substring(24, length - 20)
    # Python slicing: [24:-20]
    payload = rev[24:-20]
    
    print(f"Java Logic Payload: {payload[:50]} ...")
    
    try:
        decoded = base64.b64decode(payload).decode('utf-8')
        print("Success Decode (Java Logic):")
        print(decoded[:500]) # Print more
        return
    except Exception as e:
        print(f"Fail Decode (Java Logic): {e}")
        # Try adjusting padding
        missing_padding = len(payload) % 4
        if missing_padding:
            payload += '=' * (4 - missing_padding)
        try:
            decoded = base64.b64decode(payload).decode('utf-8')
            print("Success Decode (Java Logic + Padding):")
            print(decoded[:500])
        except Exception as e2:
             print(f"Fail Decode (Java Logic + Padding): {e2}")




sample = "Y29kZWlzcHJvdGVjdGVk==QfioVM1QjL4MjOwMjOzADV2ETL0ATL1IDMyIiOiUWbpRFZllmZpR2btJCLioVM1QjL4MjOwMjOzADV2ETL0ATL1IDMyIiOiUWbpRFZlRXYlJ3YiwiIkVmchh2crIXZkx2bm5ycwBXYtUGbn92bn5CZuZ3Lu9Wa0F2YpxGcwF2LlBXe09iNx8SbvNmL05WZ052bjJXZzVXZsd2bvdmL5RnchBHZylGa01SZ2lmck9yL6MHc0RHaiojIr5WaM52bjlmIsIiclRGbvZmLzBHch1SZsd2bvdmLk5mdv42bpRXYjlGbwBXYiojIlBXeUVWbp1mIsIya0IiOiUWbh5mIsIiTWZGeUp1bhZ2YuN0bmFnVRd3YXdWaygzSlN0MHFFUUFjI6ICZpJyeYmFzZTY0aXNleGNsdWRlZA=="
# Clean up whitespace
sample = sample.strip()

print("Testing Sample...")
decode_new_obfuscation(sample)
