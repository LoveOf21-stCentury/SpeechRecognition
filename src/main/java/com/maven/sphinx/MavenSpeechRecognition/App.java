package com.maven.sphinx.MavenSpeechRecognition;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Port;

import edu.cmu.sphinx.api.Configuration;
import edu.cmu.sphinx.api.LiveSpeechRecognizer;
import edu.cmu.sphinx.api.SpeechResult;
import edu.cmu.sphinx.result.WordResult;

public class App {
	private LiveSpeechRecognizer recognizer;
	private Logger logger = Logger.getLogger(getClass().getName());
	private String speechRecognitionResult;
	private boolean ignoreSpeechRecognitionResult = false;
	private boolean speechRecognizerThreadRunning = false;
	private boolean resourcesThreadRunning;
	private ExecutorService eventsExecutorService = Executors.newFixedThreadPool(2);

	public App() {
		logger.log(Level.INFO, "Loading Speech Recognizer...\n");
		Configuration configuration = new Configuration();
		configuration.setAcousticModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us");
		configuration.setDictionaryPath("resource:/edu/cmu/sphinx/models/en-us/cmudict-en-us.dict");
// For recognizing individual words;
//		configuration.setLanguageModelPath("resource:/edu/cmu/sphinx/models/en-us/en-us.lm.bin");
		
// For recognizing phrases from GRAM 
		configuration.setGrammarPath("resource:/grammars");
		configuration.setGrammarName("grammar");
		configuration.setUseGrammar(true);

		try {
			recognizer = new LiveSpeechRecognizer(configuration);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		startResourcesThread();
		startSpeechRecognition();

	}

	public synchronized void startSpeechRecognition() {
		if (speechRecognizerThreadRunning)
			logger.log(Level.INFO, "Speech Recognition Thread already running...\n");
		else
			eventsExecutorService.submit(() -> {
				speechRecognizerThreadRunning = true;
				ignoreSpeechRecognitionResult = false;

				recognizer.startRecognition(true);

				logger.log(Level.INFO, "You can start to speak...\n");
				try {
					while (speechRecognizerThreadRunning) {
						SpeechResult speechResult = recognizer.getResult();
						if (!ignoreSpeechRecognitionResult) {
							if (speechResult == null)
								logger.log(Level.INFO, "I can't understand what you said.\n");
							else {
								speechRecognitionResult = speechResult.getHypothesis();
								System.out.println("You said: " + speechRecognitionResult + "\n");
								printResult(speechRecognitionResult, speechResult.getWords());
							}
						} else
							logger.log(Level.INFO, "Ignoring Speech Recognition Results...");
					}
				} catch (Exception ex) {
					logger.log(Level.WARNING, null, ex);
					speechRecognizerThreadRunning = false;
				}
				logger.log(Level.INFO, "SpeechThread has exited...");
			});
	}

	public synchronized void stopIgnoreSpeechRecognitionResults() {
		ignoreSpeechRecognitionResult = false;
	}

	public synchronized void ignoreSpeechRecognitionResults() {
		ignoreSpeechRecognitionResult = true;
	}

	public void startResourcesThread() {
		if (resourcesThreadRunning)
			logger.log(Level.INFO, "Resources Thread already running...\n");
		else
			eventsExecutorService.submit(() -> {
				try {
					resourcesThreadRunning = true;
					while (true) {
						if (!AudioSystem.isLineSupported(Port.Info.MICROPHONE))
							logger.log(Level.INFO, "Microphone is not available.\n");
						Thread.sleep(350);
					}
				} catch (InterruptedException ex) {
					logger.log(Level.WARNING, null, ex);
					resourcesThreadRunning = false;
				}
			});
	}

	public void printResult(String speech, List<WordResult> speechWords) {
		System.out.println(speech);
	}

	public boolean getIgnoreSpeechRecognitionResults() {
		return ignoreSpeechRecognitionResult;
	}

	public boolean getSpeechRecognitionThreadRunning() {
		return speechRecognizerThreadRunning;
	}

	public static void main(String[] args) {
		new App();
	}
}
