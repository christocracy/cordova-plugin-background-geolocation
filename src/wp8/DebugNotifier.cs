using System;
using System.Collections.Generic;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Phone.Shell;
using Microsoft.Xna.Framework;
using Microsoft.Xna.Framework.Audio;

namespace Cordova.Extension.Commands
{
    public interface IDebugNotifier
    {
        /// <summary>
        /// Play a single tone in the background
        /// </summary>
        /// <param name="tone">The tone</param>
        void Notify(Tone tone);

        /// <summary>
        /// Plays audio tones in the background with a given pause between them
        /// </summary>
        /// <param name="millisecondsPauseBetween">Milliseconds pause between the tones</param>
        /// <param name="tones">Audio tones to play</param>
        void Notify(int millisecondsPauseBetween, params Tone[] tones);

        /// <summary>
        /// Shows a toast message at the top of the screen, (only) when the app is in background
        /// </summary>
        /// <param name="toastMessage">Message shown in toast</param> 
        void Notify(string toastMessage);

        /// <summary>
        /// Shows a toast message at the top of the screen, (only) when the app is in background. At the same time one or more tones are played
        /// </summary>
        /// <param name="toastMessage">Message shown in toast</param>
        /// <param name="tones">Audio tones to play</param>
        void Notify(string toastMessage, params Tone[] tones); 
    }

    public sealed class DebugNotifier : IDebugNotifier
    {
        // Singleton
        private static DebugNotifier _debugNotifier;
        private DebugNotifier() { }
        public static DebugNotifier GetDebugNotifier() { return _debugNotifier ?? (_debugNotifier = new DebugNotifier()); }

        private DynamicSoundEffectInstance _dynamicSound;
        private DispatcherTimer _dispatcherTimer;

        private readonly Queue<Tone> _toneQueue = new Queue<Tone>();

        private int _currentToneFrequency;
        private int _currentToneMillisecondsLeft;

        /// <summary>
        /// More info on next property and function: http://msdn.microsoft.com/library/windows/apps/jj662938%28v=vs.105%29.aspx
        /// Set the minimum version number that supports custom toast sounds
        /// Followed by a function to determine if the current device is running the target version.
        /// </summary>
        private static readonly Version TargetVersion = new Version(8, 0, 10492);
        private static bool IsTargetedVersion
        {
            get { return Environment.OSVersion.Version >= TargetVersion; }
        }

        public void Notify(Tone tone)
        {
            PlayTones(0, tone);
        }

        public void Notify(int millisecondsPauseBetween, params Tone[] tones)
        {
            PlayTones(millisecondsPauseBetween, tones);
        }

        public void Notify(string toastMessage)
        {
            ShowToast(toastMessage);
        }

        public void Notify(string toastMessage, params Tone[] tones)
        {
            PlayTones(300, tones);
            ShowToast(toastMessage);
        }

        private void PlayTones(int millisecondsPauseBetween, params Tone[] tones)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                SetupDispatcherTimer();

                _toneQueue.Clear();
                for (var t = 0; t < tones.Length; t++)
                {
                    _toneQueue.Enqueue(tones[t]);

                    // 'Pause tone' (silence) between
                    if (t != tones.Length - 1) _toneQueue.Enqueue(new Tone { Frequency = tones[t].Frequency, DurationInMilliSeconds = millisecondsPauseBetween, Mute = true });
                }

                FrameworkDispatcher.Update();
                UpdateCurrentTone();
                _dispatcherTimer.Start();
            });
        }

        private void SetupDispatcherTimer()
        {
            if (_dispatcherTimer == null)
            {
                _dispatcherTimer = new DispatcherTimer
                {
                    Interval = TimeSpan.FromMilliseconds(33)
                };
                _dispatcherTimer.Tick += delegate
                {
                    FrameworkDispatcher.Update();
                    UpdateCurrentTone();
                };
            }

            if (_dispatcherTimer.IsEnabled) _dispatcherTimer.Stop();
        }

        private void UpdateCurrentTone()
        {
            if (_currentToneMillisecondsLeft > 0)
            {
                _currentToneMillisecondsLeft -= _dispatcherTimer.Interval.Milliseconds;
                return;
            }

            if (_dynamicSound != null) _dynamicSound.Stop(); // otherwise, buffer (1sec) is played out

            if (_toneQueue.Count == 0)
            {
                _dispatcherTimer.Stop();
                return;
            }

            var newTone = _toneQueue.Dequeue();
            _currentToneMillisecondsLeft = newTone.DurationInMilliSeconds - _dispatcherTimer.Interval.Milliseconds;

            if (newTone.Mute) return;

            _currentToneFrequency = newTone.Frequency;

            _dynamicSound = new DynamicSoundEffectInstance(48000, AudioChannels.Stereo);
            _dynamicSound.BufferNeeded += GetSoundBuffer;

            _dynamicSound.Play();
        }

        private void GetSoundBuffer(object sender, EventArgs e)
        {
            var buffer = new byte[_dynamicSound.GetSampleSizeInBytes(TimeSpan.FromSeconds(1))];
            const double sampleSize = 48000 * 2;
            const double t = (Math.PI) / sampleSize;

            for (long i = 0; (i + 1) < buffer.Length; i += 2)
            {
                var theta = i * t * _currentToneFrequency;
                float amplitude = (int)(Math.Sin((float)theta) * 64);
                var oldAmplitude = (float)((buffer[i] << 8) | buffer[i + 1]);
                amplitude = (amplitude + oldAmplitude) * 0.5f;
                var data = (int)amplitude;
                buffer[i] = (byte)(data >> 8);
                buffer[i + 1] = (byte)data;
            }

            _dynamicSound.SubmitBuffer(buffer);
        }

        private void ShowToast(string message)
        {
            var toast = new ShellToast
            {
                Title = "GPS",
                Content = message
            };
            if (IsTargetedVersion) SetProperty(toast, "Sound", new Uri("", UriKind.RelativeOrAbsolute));
            toast.Show();
        }

        private static void SetProperty(object instance, string name, object value)
        {
            var setMethod = instance.GetType().GetProperty(name).GetSetMethod();
            setMethod.Invoke(instance, new[] { value });
        }
    }

    public class Tone
    {
        public Tone()
        {
            Mute = false;
        }

        public Tone(int durationInMilliSeconds, int frequency)
        {
            Mute = false;
            DurationInMilliSeconds = durationInMilliSeconds;
            Frequency = frequency;
        }

        public Tone(int durationInMilliSeconds, Frequency frequency)
            : this(durationInMilliSeconds, (int)frequency)
        {
        }

        public int DurationInMilliSeconds;
        public int Frequency;
        public bool Mute;
    }

    public enum Frequency
    {
        Low = 300,
        High = 900
    }
}