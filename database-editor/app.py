from flask import Flask, render_template, jsonify
import os

app = Flask(__name__, static_folder='static', template_folder='templates')
app.config.setdefault('DEBUG', False)


@app.route('/')
def index():
    return render_template('index.html')


@app.route('/health')
def health():
    return jsonify({'status': 'ok'})


if __name__ == '__main__':
    # development server
    print('Starting static server on http://127.0.0.1:5000')
    app.run(debug=app.config['DEBUG'])
