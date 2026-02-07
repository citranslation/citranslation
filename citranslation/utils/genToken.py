import os
from dotenv import load_dotenv




def github_token():
    load_dotenv()
    api_key = os.getenv("GITHUB_TOKEN")
    return api_key



def openai_token():
    load_dotenv()
    api_key = os.getenv("OPENAI_API_KEY")
    return api_key

def api_token():
    load_dotenv()
    api_key = os.getenv("API_KEY")
    return api_key