using System;
using System.Windows;
using System.Windows.Threading;
using Microsoft.Xna.Framework;
using Microsoft.Xna.Framework.Audio;

namespace Cordova.Extension.Commands
{
    public class DebugAudioNotifier : IDisposable
    {
        private static DebugAudioNotifier _audioNotifier;

        private DynamicSoundEffectInstance _dynamicSound;
        private DispatcherTimer _timer;
        private TimeSpan _timeLeft;

        private const int SampleRate = 48000;
        private Tone _frequency = Tone.Low;

        private int _bufferSize;
        private byte[] _soundBuffer;
        private int _totalTime;


        public enum Tone
        {
            Low = 300,
            High = 900
        }

        private DebugAudioNotifier()
        {

        }

        public static DebugAudioNotifier GetDebugAudioNotifier()
        {
            return _audioNotifier ?? (_audioNotifier = new DebugAudioNotifier());
        }

        public void PlaySound(Tone tone, TimeSpan duration)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() =>
            {
                if (_timer == null)
                {
                    _timer = new DispatcherTimer
                    {
                        Interval = TimeSpan.FromMilliseconds(33)
                    };
                    _timer.Tick += delegate { try { FrameworkDispatcher.Update(); } catch { } };
                }

                if (_timer.IsEnabled) _timer.Stop();

                _timeLeft = duration;

                FrameworkDispatcher.Update();
                _frequency = tone;
                _dynamicSound = new DynamicSoundEffectInstance(SampleRate, AudioChannels.Mono);
                _dynamicSound.BufferNeeded += dynamicSound_BufferNeeded;
                _dynamicSound.Play();
                _bufferSize = _dynamicSound.GetSampleSizeInBytes(TimeSpan.FromSeconds(1));
                _soundBuffer = new byte[_bufferSize];

                _timer.Start();
            });
        }

        private void dynamicSound_BufferNeeded(object sender, EventArgs e)
        {
            for (var i = 0; i < _bufferSize - 1; i += 2)
            {
                var time = _totalTime / (double)SampleRate;
                var sample = (short)(Math.Sin(2 * Math.PI * (int)_frequency * time) * short.MaxValue);
                _soundBuffer[i] = (byte)sample;
                _soundBuffer[i + 1] = (byte)(sample >> 8);
                _totalTime++;
            }
            _timeLeft = _timeLeft.Subtract(TimeSpan.FromSeconds(_totalTime / SampleRate));
            if (_timeLeft.Ticks <= 0)
            {
                _totalTime = 0;
                return;
            }

            _dynamicSound.SubmitBuffer(_soundBuffer);
        }

        public void Dispose()
        {
            _dynamicSound.Dispose();
        }
    }
}