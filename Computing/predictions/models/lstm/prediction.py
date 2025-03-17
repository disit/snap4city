''' Snap4city Computing PREDICTIONS.
   Copyright (C) 2024 DISIT Lab http://www.disit.org - University of Florence

   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU Affero General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU Affero General Public License for more details.
   You should have received a copy of the GNU Affero General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
'''

import numpy as np
import pickle
from tensorflow.keras.models import load_model
import os

def lstm(past_data: list):

    script_dir = os.path.dirname(os.path.abspath(__file__))
    scaler_path = os.path.join(script_dir, 'scaler.pkl')
    model_path = os.path.join(script_dir, 'lstm_model.h5')

    # Carichiamo lo scaler salvato
    with open(scaler_path, 'rb') as file:
        loaded_scaler = pickle.load(file)

    past_data = np.array(past_data).reshape(-1, 1)
    scaled_past_data = loaded_scaler.transform(past_data).reshape(1, -1, 1)
    
    loaded_model = load_model(model_path)
    scaled_predictions = loaded_model.predict(scaled_past_data)
    scaled_predictions = scaled_predictions.reshape(-1, 1)
    
    predictions = loaded_scaler.inverse_transform(scaled_predictions)
    
    return predictions.tolist()[0]

if __name__ == '__main__':
    past_data =[[[0.43982024],
        [0.43982024],
        [0.43982024],
        [0.43982024],
        [0.43982024],
        [0.42696208],
        [0.42696208],
        [0.42696208],
        [0.42696208],
        [0.42696208],
        [0.42696208],
        [0.21654164],
        [0.21654164],
        [0.21654164],
        [0.21654164],
        [0.21654164],
        [0.21654164],
        [0.43017581],
        [0.43017581],
        [0.43017581],
        [0.43017581],
        [0.43017581],
        [0.43017581],
        [0.35932251],
        [0.35932251],
        [0.35932251],
        [0.35932251],
        [0.35932251],
        [0.35932251],
        [0.41078421],
        [0.41078421],
        [0.41078421],
        [0.41078421],
        [0.41078421],
        [0.41078421],
        [0.43188942],
        [0.43188942],
        [0.43188942],
        [0.43188942],
        [0.43188942],
        [0.43188942],
        [0.3888925 ],
        [0.3888925 ],
        [0.3888925 ],
        [0.3888925 ],
        [0.3888925 ],
        [0.3888925 ],
        [0.363414  ],
        [0.363414  ],
        [0.363414  ],
        [0.363414  ],
        [0.363414  ],
        [0.363414  ],
        [0.40949929],
        [0.40949929],
        [0.40949929],
        [0.40949929],
        [0.40949929],
        [0.40949929],
        [0.00061   ],
        [0.        ],
        [0.00061   ],
        [0.        ],
        [0.00061   ],
        [0.00061   ],
        [0.        ],
        [0.        ],
        [0.        ],
        [0.        ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.        ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.        ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.00061   ],
        [0.16262989],
        [0.16262989],
        [0.16262989],
        [0.16262989],
        [0.16262989],
        [0.16262989],
        [0.17489823],
        [0.17489823],
        [0.17489823],
        [0.17489823],
        [0.17489823]]]
    lstm(past_data)