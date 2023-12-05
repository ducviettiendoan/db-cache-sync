import requests, json, sys, time

url = "http://localhost:8085/student/add"

headers = {
  'Content-Type': 'application/json'
}

def test_batch(start,end):
  for i in range(start,end+1):
    response = requests.request("POST", url, 
                                headers=headers, 
                                data=json.dumps({
                                  "id": i,
                                  "name": f"S{i}",
                                  "age": i+5
                                }))
    time.sleep(0.5)
  return "Run Complete!"

def main():
  try:
    start = sys.argv[1]
    end = sys.argv[2]
    print(start,end)
    print(test_batch(int(start),int(end)))
  except:
    raise Exception("Need at least 2 following args after file_name to run function")
main()
