using System;

namespace Cordova.Extension.Commands
{
    public class UpdateScaledDistanceFilterResult
    { 
        public double InitialScaledDistanceFilter { get; set; }
        public double NewScaledDistanceFilter { get; set; } 
        public bool SkipPositionBecauseOfDistance { get; set; }
        public bool StartStationary { get; set; }
        public double? Distance { get; set; }

        public bool ScaledDistanceFilterChanged
        {
            get
            {
                return Math.Abs(InitialScaledDistanceFilter) < 0.1 || Math.Abs(InitialScaledDistanceFilter - NewScaledDistanceFilter) > 1;
            }
        }

        public UpdateScaledDistanceFilterResult(double initialScaledDistanceFilter)
        {
            InitialScaledDistanceFilter = initialScaledDistanceFilter;
            NewScaledDistanceFilter = initialScaledDistanceFilter;
        }
    }
}