from flask import Flask, request, Response
import openai
import json

app = Flask(__name__)

# Configure the OpenAI client to point to the local server
client = openai.OpenAI(base_url="http://localhost:1234/v1", api_key="not-needed")

@app.route('/chat', methods=['POST'])
def chat():
    # Get the history and the user's message from the request body
    data = request.json
    history = data.get('history')

    def generate_responses():
        try:
            # Call the local model
            completions = client.chat.completions.create(
                model="local-model", # adjust as necessary
                messages=history,
                temperature=0.7,
                stream=True
            )

            # Process each partial response as it arrives
            for completion in completions:
                if completion.choices[0].delta.content:
                    yield json.dumps(completion.choices[0].delta.content) + "\n"

        except Exception as e:
            yield json.dumps({'error': str(e)}) + "\n"

    return Response(generate_responses(), mimetype='text/plain')

if __name__ == '__main__':
    app.run(debug=True, port=5000)