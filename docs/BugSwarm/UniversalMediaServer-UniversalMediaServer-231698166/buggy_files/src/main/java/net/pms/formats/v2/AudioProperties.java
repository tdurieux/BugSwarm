/*
 * PS3 Media Server, for streaming any media to your PS3.
 * Copyright (C) 2012  I. Sokolov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.formats.v2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for storing and parsing from libmediainfo output audio track's properties.
 * (bitrate, channels number, etc) and meta information (title, album, etc).
 *
 * This class is not thread-safe.
 *
 * @since 1.60.0
 */
public class AudioProperties {
	private static final Logger LOGGER = LoggerFactory.getLogger(AudioProperties.class);

	private static final Pattern intPattern = Pattern.compile("([\\+-]?\\d+)([eE][\\+-]?\\d+)?");
//	private static final Pattern floatPattern = Pattern.compile("([\\+-]?\\d(\\.\\d*)?|\\.\\d+)([eE][\\+-]?(\\d(\\.\\d*)?|\\.\\d+))?");

	private int bitRate = 8000;
	private int numberOfChannels = 2;
	private int audioDelay = 0;
	private int sampleFrequency = 48000;
	private int bitsperSample = 16;

	public int getAttribute(AudioAttribute attribute) {
		switch (attribute) {
			case BITRATE:
				return getBitRate();
			case CHANNELS_NUMBER:
				return getNumberOfChannels();
			case DELAY:
				return getAudioDelay();
			case SAMPLE_FREQUENCY:
				return getSampleFrequency();
			case BITS_PERSAMPLE:
				return getBitsperSample();
			default:
				throw new IllegalArgumentException("Unimplemented attribute");
		}
	}

	/**
	 * Get bitrate for this audio track.
	 * @return bitrate (default is 8000).
	 */
	public int getBitRate() {
		return bitRate;
	}

	/**
	 * Set bitrate for this audio track.
	 *
	 * @param bitrate to set.
	 */
	public void setBitRate(int bitRate) {
		this.bitRate = bitRate;
	}

	/**
	 * Set bitrate for this audio track with libmediainfo value.
	 *
	 * @param mediaInfoValue libmediainfo "BitRate" value to parse.
	 */
	public void setBitRate(String mediaInfoValue) {
		this.bitRate = getBitRateFromLibMediaInfo(mediaInfoValue);
	}

	/**
	 * Get number of channels for this audio track.
	 * @return number of channels (default is 2).
	 */
	public int getNumberOfChannels() {
		return numberOfChannels;
	}

	/**
	 * Set number of channels for this audio track.
	 *
	 * @param numberOfChannels number of channels to set.
	 */
	public void setNumberOfChannels(int numberOfChannels) {
		this.numberOfChannels = numberOfChannels;
	}

	/**
	 * Set number of channels for this audio track with libmediainfo value.
	 *
	 * @param mediaInfoValue libmediainfo "Channel(s)" value to parse.
	 */
	public void setNumberOfChannels(String mediaInfoValue) {
		this.numberOfChannels = getChannelsNumberFromLibMediaInfo(mediaInfoValue);
	}

	/**
	 * Get bits per sample for this audio track.
	 * @return bits per sample (default is 16).
	 */
	public int getBitsperSample() {
		return bitsperSample;
	}

	/**
	 * Set bits per sample for this audio track.
	 *
	 * @param bitsperSample bits per sample to set.
	 */
	public void setBitsperSample(int bitsperSample) {
		this.bitsperSample = bitsperSample;
	}

	/**
	 * Set bits per sample for this audio track with libmediainfo value.
	 *
	 * @param mediaInfoValue libmediainfo "BitDepth" value to parse.
	 */
	public void setBitsperSample(String mediaInfoValue) {
		this.bitsperSample = getBitsperSampleFromLibMediaInfo(mediaInfoValue);
	}

	/**
	 * Get delay for this audio track.
	 * @return audio delay in ms. May be negative.
	 */
	public int getAudioDelay() {
		return audioDelay;
	}

	/**
	 * Set delay for this audio track.
	 *
	 * @param audioDelay audio delay in ms to set. May be negative.
	 */
	public void setAudioDelay(int audioDelay) {
		this.audioDelay = audioDelay;
	}

	/**
	 * Set delay for this audio track with libmediainfo value.
	 *
	 * @param mediaInfoValue libmediainfo "Video_Delay" value to parse.
	 */
	public void setAudioDelay(String mediaInfoValue) {
		this.audioDelay = getAudioDelayFromLibMediaInfo(mediaInfoValue);
	}

	/**
	 * Get sample frequency for this audio track.
	 * @return sample frequency in Hz.
	 */
	public int getSampleFrequency() {
		return sampleFrequency;
	}

	/**
	 * Set sample frequency for this audio track.
	 *
	 * @param sampleFrequency sample frequency in Hz
	 */
	public void setSampleFrequency(int sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	/**
	 * Set sample frequency for this audio track with libmediainfo value.
	 *
	 * @param mediaInfoValue libmediainfo "Sampling rate" value to parse.
	 */
	public void setSampleFrequency(String mediaInfoValue) {
		this.sampleFrequency = getSampleFrequencyFromLibMediaInfo(mediaInfoValue);
	}

	public static int getChannelsNumberFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 2.");
			return 2;
		}

		// examples of libmediainfo  (mediainfo --Full --Language=raw file):
		// Channel(s) : 2
		// Channel(s) : 6
		// Channel(s) : 2 channels / 1 channel / 1 channel

		int result = -1;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		while (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				int currentResult = Integer.parseInt(matchResult);
				if (currentResult > result) {
					result = currentResult;
				}
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}

		if (result <= 0) {
			LOGGER.warn("Can't parse value {}. Returning default number 2.", mediaInfoValue);
			return 2;
		} else {
			return result;
		}
	}

	public static int getBitsperSampleFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 16.");
			return 16;
		}

		// examples of libmediainfo  (mediainfo --Full --Language=raw file):
		// Bit depth : 16
		// Bit depth : 24
		// Bit depth : / 24 / 24

		int result = -1;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		while (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				int currentResult = Integer.parseInt(matchResult);
				if (currentResult > result) {
					result = currentResult;
				}
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}

		if (result <= 0) {
			LOGGER.warn("Can't parse value {}. Returning default number 16.", mediaInfoValue);
			return 16;
		} else {
			return result;
		}
	}


	public static int getAudioDelayFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 0.");
			return 0;
		}

		// examples of libmediainfo output (mediainfo --Full --Language=raw file):
		// Video_Delay : -408

		int result = 0;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		if (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				result = Integer.parseInt(matchResult);
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}
		return result;
	}

	public static int getSampleFrequencyFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 48000 Hz.");
			return 48000;
		}

		// examples of libmediainfo output (mediainfo --Full --Language=raw file):
		// SamplingRate : 48000
		// SamplingRate : 44100 / 22050

		int result = -1;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		while (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				int currentResult = Integer.parseInt(matchResult);
				if (currentResult > result) {
					result = currentResult;
				}
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}

		if (result < 1) {
			LOGGER.warn("Can't parse value {}. Returning default number 48000 Hz.", mediaInfoValue);
			return 48000;
		} else {
			return result;
		}
	}

	public static int getBitRateFromLibMediaInfo(String mediaInfoValue) {
		if (isEmpty(mediaInfoValue)) {
			LOGGER.warn("Empty value passed in. Returning default number 8000.");
			return 8000;
		}

		// examples of libmediainfo output (mediainfo --Full --Language=raw file):
		// BitRate : 1509000
		// BitRate : Unknown / Unknown / 1509000

		int result = -1;
		Matcher intMatcher = intPattern.matcher(mediaInfoValue);
		while (intMatcher.find()) {
			String matchResult = intMatcher.group();
			try {
				int currentResult = Integer.parseInt(matchResult);
				if (currentResult > result) {
					result = currentResult;
				}
			} catch (NumberFormatException ex) {
				LOGGER.warn("NumberFormatException during parsing substring {} from value {}", matchResult, mediaInfoValue);
			}
		}

		if (result < 1) {
			LOGGER.warn("Can't parse value {}. Returning default number 8000.", mediaInfoValue);
			return 8000;
		} else {
			return result;
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Bitrate: ").append(getBitRate());
		if (getNumberOfChannels() == 1) {
			result.append(", Channel: ").append(getNumberOfChannels());
		} else {
			result.append(", Channels: ").append(getNumberOfChannels());
		}
		result.append(", Sample Frequency: ").append(getSampleFrequency()).append(" Hz");
		if (getBitsperSample() != 16) {
			result.append(", Bits per Sample: ").append(getBitsperSample());
		}
		if (getAudioDelay() != 0) {
			result.append(", Delay: ").append(getAudioDelay()).append(" ms");
		}

		return result.toString();
	}
}
